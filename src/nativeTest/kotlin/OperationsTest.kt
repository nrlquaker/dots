import com.github.ajalt.clikt.testing.test
import commands.Backup
import commands.Env
import config.Config
import config.Entry
import config.LoadResult
import config.ResolvedEntry
import fs.Operations
import fs.PlanResult
import fs.Status
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OperationsTest {
    private val fs = FakeFileSystem().apply { allowSymlinks = true }
    private val ops = Operations(fs) { it.toString() }
    private val source = "/home/.zshrc".toPath()
    private val target = "/dotfiles/shell/zshrc".toPath()
    private val entry = ResolvedEntry("zsh", source, target)

    private fun file(path: Path, content: String = "data") {
        fs.createDirectories(path.parent!!)
        fs.write(path) { writeUtf8(content) }
    }

    private fun read(path: Path) = fs.read(path) { readUtf8() }

    private fun apply(result: PlanResult) = assertIs<PlanResult.Actions>(result).actions.forEach(ops::execute)

    private fun linkTarget(path: Path) = fs.metadata(path).symlinkTarget

    private fun backedUp() {
        file(source)
        apply(ops.planBackup(entry, overwrite = false))
    }

    @Test
    fun backupMovesAndLinks() {
        backedUp()
        assertEquals("data", read(target))
        assertEquals(target, linkTarget(source))
        assertEquals(Status.LINKED, ops.status(entry))
    }

    @Test
    fun backupIsNoOpWhenAlreadyLinked() {
        backedUp()
        assertEquals(PlanResult.Actions(emptyList(), "already backed up"), ops.planBackup(entry, overwrite = false))
    }

    @Test
    fun backupConflictsOnExistingTarget() {
        file(source, "new")
        file(target, "old")
        assertIs<PlanResult.Error>(ops.planBackup(entry, overwrite = false))
        apply(ops.planBackup(entry, overwrite = true))
        assertEquals("new", read(target))
        assertEquals(target, linkTarget(source))
    }

    @Test
    fun backupMovesDirectories() {
        val dir = ResolvedEntry("nvim", "/home/.config/nvim".toPath(), "/dotfiles/nvim".toPath())
        file(dir.source / "init.lua", "lua")
        apply(ops.planBackup(dir, overwrite = false))
        assertEquals("lua", read(dir.target / "init.lua"))
        assertEquals("lua", read(dir.source / "init.lua"))
    }

    @Test
    fun backupFailsOnMissingSource() {
        assertIs<PlanResult.Error>(ops.planBackup(entry, overwrite = true))
    }

    @Test
    fun restoreUndoesBackup() {
        backedUp()
        apply(ops.planRestore(entry, overwrite = false))
        assertEquals(null, linkTarget(source))
        assertEquals("data", read(source))
        assertTrue(!fs.exists(target))
    }

    @Test
    fun restoreConflictsOnRealSourceFile() {
        file(source, "local")
        file(target, "saved")
        assertIs<PlanResult.Error>(ops.planRestore(entry, overwrite = false))
        apply(ops.planRestore(entry, overwrite = true))
        assertEquals("saved", read(source))
    }

    @Test
    fun restoreFailsOnMissingTarget() {
        file(source)
        assertIs<PlanResult.Error>(ops.planRestore(entry, overwrite = true))
    }

    @Test
    fun syncLinksExistingTarget() {
        file(target)
        assertEquals(Status.NOT_LINKED, ops.status(entry))
        apply(ops.planSync(entry, overwrite = false))
        assertEquals(target, linkTarget(source))
        assertEquals(PlanResult.Actions(emptyList(), "already linked"), ops.planSync(entry, overwrite = false))
    }

    @Test
    fun syncConflictsOnLinkElsewhere() {
        file(target)
        fs.createDirectories(source.parent!!)
        fs.createSymlink(source, "/elsewhere".toPath())
        assertEquals(Status.CONFLICT, ops.status(entry))
        assertIs<PlanResult.Error>(ops.planSync(entry, overwrite = false))
        apply(ops.planSync(entry, overwrite = true))
        assertEquals(target, linkTarget(source))
    }

    @Test
    fun cleanRemovesOnlyManagedBrokenLinks() {
        backedUp()
        fs.delete(target)
        val unrelated = "/home/.other".toPath()
        fs.createSymlink(unrelated, "/gone".toPath())
        assertEquals(Status.BROKEN, ops.status(entry))
        apply(ops.planClean(entry))
        assertTrue(fs.metadataOrNull(source) == null)
        assertTrue(fs.metadataOrNull(unrelated) != null)
    }

    @Test
    fun cleanIgnoresHealthyLinks() {
        backedUp()
        assertEquals(PlanResult.Actions(emptyList(), "nothing to clean"), ops.planClean(entry))
    }

    @Test
    fun preflightErrorLeavesFilesystemUntouched() {
        val home = "/home".toPath()
        file(source, "ok")
        file("/home/.vimrc".toPath(), "vim")
        file("/dotfiles/vimrc".toPath(), "existing")
        val config = Config(
            dots = mapOf(
                "zsh" to Entry("~/.zshrc", target.toString()),
                "vim" to Entry("~/.vimrc", "/dotfiles/vimrc"),
                "git" to Entry("~/.gitconfig", "/dotfiles/gitconfig"),
            )
        )
        val result = Backup(Env(fs, home, LoadResult.Loaded(config))).test("")
        assertEquals(1, result.statusCode)
        assertContains(result.stdout, "✗ vim")
        assertContains(result.stdout, "✗ git")
        assertContains(result.stdout, "• zsh")
        assertContains(result.stdout, "===> Backup failed: nothing was changed")
        assertEquals("ok", read(source))
        assertTrue(!fs.exists(target))
    }

    @Test
    fun reportsChangedAndUnchangedEntries() {
        file(source)
        val vim = ResolvedEntry("vim", "/home/.vimrc".toPath(), "/dotfiles/vimrc".toPath())
        file(vim.source)
        apply(ops.planBackup(vim, overwrite = false))
        val config = Config(
            dots = mapOf(
                "zsh" to Entry("~/.zshrc", target.toString()),
                "vim" to Entry("~/.vimrc", vim.target.toString()),
            )
        )
        val result = Backup(Env(fs, "/home".toPath(), LoadResult.Loaded(config))).test("")
        assertEquals(0, result.statusCode)
        assertContains(result.stdout, "✓ zsh  ~/.zshrc -> /dotfiles/shell/zshrc   moved to target and linked")
        assertContains(result.stdout, "• vim  ~/.vimrc -> /dotfiles/vimrc")
        assertContains(result.stdout, "===> Backup successful: 1 changed, 1 unchanged")
    }
}
