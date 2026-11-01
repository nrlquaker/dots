import com.github.ajalt.clikt.testing.test
import commands.Backup
import commands.Env
import config.CommandOptions
import config.Config
import config.Entry
import config.LoadResult
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals

/** Backup over an existing target succeeds only when overwrite resolves to true. */
class PrecedenceTest {
    private val source = "/home/.zshrc".toPath()
    private val target = "/dotfiles/zshrc".toPath()

    private fun backup(options: CommandOptions?, args: String): Int {
        val fs = FakeFileSystem().apply { allowSymlinks = true }
        listOf(source, target).forEach {
            fs.createDirectories(it.parent!!)
            fs.write(it) { writeUtf8("x") }
        }
        val config = Config(backup = options, dots = mapOf("zsh" to Entry(source.toString(), target.toString())))
        return Backup(Env(fs, "/home".toPath(), LoadResult.Loaded(config))).test(args).statusCode
    }

    @Test
    fun cliOverwriteBeatsConfigFalse() = assertEquals(0, backup(CommandOptions(overwrite = false), "--overwrite"))

    @Test
    fun cliNoOverwriteBeatsConfigTrue() = assertEquals(1, backup(CommandOptions(overwrite = true), "--no-overwrite"))

    @Test
    fun configTrueAppliesWithoutFlag() = assertEquals(0, backup(CommandOptions(overwrite = true), ""))

    @Test
    fun configFalseAppliesWithoutFlag() = assertEquals(1, backup(CommandOptions(overwrite = false), ""))

    @Test
    fun defaultsToFalseWithoutConfigValue() = assertEquals(1, backup(null, ""))

    @Test
    fun shortFlagWorks() = assertEquals(0, backup(null, "-o"))
}
