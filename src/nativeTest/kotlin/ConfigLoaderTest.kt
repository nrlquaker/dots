import com.github.ajalt.clikt.testing.test
import commands.Env
import commands.Init
import commands.ListCommand
import config.Config
import config.ConfigLoader
import config.Entry
import config.InvalidEntriesException
import config.LoadResult
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ConfigLoaderTest {
    private val home = "/Users/me".toPath()
    private val configPath = ConfigLoader.configPath(home)
    private val fs = FakeFileSystem().apply { allowSymlinks = true; createDirectories(home) }

    private fun writeConfig(json: String) {
        fs.createDirectories(configPath.parent!!)
        fs.write(configPath) { writeUtf8(json) }
    }

    @Test
    fun missingConfig() {
        assertEquals(LoadResult.Missing(configPath), ConfigLoader.load(fs, home))
    }

    @Test
    fun malformedJson() {
        writeConfig("{ not json")
        assertEquals(LoadResult.Invalid(configPath), ConfigLoader.load(fs, home))
    }

    @Test
    fun unknownKeyIsInvalid() {
        writeConfig("""{ "backup": { "verbose": true }, "dots": {} }""")
        assertEquals(LoadResult.Invalid(configPath), ConfigLoader.load(fs, home))
    }

    @Test
    fun missingDotsIsInvalid() {
        writeConfig("""{ "sync": { "overwrite": true } }""")
        assertEquals(LoadResult.Invalid(configPath), ConfigLoader.load(fs, home))
    }

    @Test
    fun malformedConfigShowsShortMessage() {
        writeConfig("""{ "backup": { "verbose": false }, "dots": {} }""")
        val result = ListCommand(Env(fs, home, ConfigLoader.load(fs, home))).test("")
        assertEquals(1, result.statusCode)
        assertEquals(
            "Config at ~/.config/dots/config is malformed. Fix it or run `dots init --force` to start over.\n",
            result.stderr,
        )
    }

    @Test
    fun loadsOptionsAndEntries() {
        writeConfig("""{ "sync": { "overwrite": true }, "dots": { "zsh": { "source": "~/.zshrc", "target": "/d/zshrc" } } }""")
        val config = assertIs<LoadResult.Loaded>(ConfigLoader.load(fs, home)).config
        assertEquals(true, config.sync?.overwrite)
        assertEquals(null, config.backup)
        assertEquals(Entry("~/.zshrc", "/d/zshrc"), config.dots["zsh"])
    }

    @Test
    fun expandsTilde() {
        val entries = ConfigLoader.resolveEntries(config("zsh" to Entry("~/.zshrc", "~/dotfiles/zshrc")), home)
        assertEquals("/Users/me/.zshrc".toPath(), entries.single().source)
        assertEquals("/Users/me/dotfiles/zshrc".toPath(), entries.single().target)
    }

    @Test
    fun rejectsRelativePaths() {
        val e = assertFailsWith<InvalidEntriesException> {
            ConfigLoader.resolveEntries(config("zsh" to Entry(".zshrc", "~user/zshrc")), home)
        }
        assertEquals(2, e.errors.size)
    }

    @Test
    fun rejectsDuplicatesAndSamePath() {
        val e = assertFailsWith<InvalidEntriesException> {
            ConfigLoader.resolveEntries(
                config(
                    "a" to Entry("~/a", "/d/a"),
                    "b" to Entry("~/a", "/d/a"),
                    "c" to Entry("~/c", "~/c"),
                ),
                home,
            )
        }
        assertEquals(3, e.errors.size)
    }

    @Test
    fun initWritesLoadableTemplate() {
        val result = Init(Env(fs, home, ConfigLoader.load(fs, home))).test("")
        assertEquals(0, result.statusCode)
        val config = assertIs<LoadResult.Loaded>(ConfigLoader.load(fs, home)).config
        assertEquals(emptyMap(), config.dots)
    }

    @Test
    fun initRefusesExistingConfigUnlessForced() {
        writeConfig("custom")
        val env = Env(fs, home, ConfigLoader.load(fs, home))
        assertEquals(1, Init(env).test("").statusCode)
        assertEquals("custom", fs.read(configPath) { readUtf8() })
        assertEquals(0, Init(env).test("--force").statusCode)
        assertIs<LoadResult.Loaded>(ConfigLoader.load(fs, home))
    }

    @Test
    fun commandWithoutConfigPointsToInit() {
        val result = ListCommand(Env(fs, home, ConfigLoader.load(fs, home))).test("")
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, "dots init")
    }

    private val repoConfig = home / "dotfiles" / "dots" / "config"

    private fun writeRepoConfig(json: String = """{ "dots": {} }""") {
        fs.createDirectories(repoConfig.parent!!)
        fs.write(repoConfig) { writeUtf8(json) }
    }

    private fun init(args: String) = Init(Env(fs, home, ConfigLoader.load(fs, home))).test(args)

    @Test
    fun initLinksExistingConfig() {
        writeRepoConfig()
        val result = init("$repoConfig")
        assertEquals(0, result.statusCode)
        assertEquals(repoConfig, fs.metadataOrNull(configPath)?.symlinkTarget)
        assertIs<LoadResult.Loaded>(ConfigLoader.load(fs, home))
        assertContains(result.stdout, "Linked ~/.config/dots/config -> ~/dotfiles/dots/config")
    }

    @Test
    fun initResolvesRelativeAndTildePaths() {
        writeRepoConfig()
        fs.workingDirectory = home / "dotfiles"
        assertEquals(0, init("dots/config").statusCode)
        assertEquals(repoConfig, fs.metadataOrNull(configPath)?.symlinkTarget)
        assertEquals(0, init("--force ~/dotfiles/dots/config").statusCode)
        assertEquals(repoConfig, fs.metadataOrNull(configPath)?.symlinkTarget)
    }

    @Test
    fun initLinkRefusesExistingConfigUnlessForced() {
        writeRepoConfig()
        writeConfig("custom")
        assertEquals(1, init("$repoConfig").statusCode)
        assertEquals("custom", fs.read(configPath) { readUtf8() })
        assertEquals(0, init("--force $repoConfig").statusCode)
        assertEquals(repoConfig, fs.metadataOrNull(configPath)?.symlinkTarget)
    }

    @Test
    fun brokenConfigLinkIsMissing() {
        fs.createDirectories(configPath.parent!!)
        fs.createSymlink(configPath, "/gone/config".toPath())
        assertEquals(LoadResult.Missing(configPath), ConfigLoader.load(fs, home))
    }

    @Test
    fun initReplacesBrokenLinkWithoutForce() {
        writeRepoConfig()
        fs.createDirectories(configPath.parent!!)
        fs.createSymlink(configPath, "/gone/config".toPath())
        assertEquals(0, init("$repoConfig").statusCode)
        assertEquals(repoConfig, fs.metadataOrNull(configPath)?.symlinkTarget)
    }

    @Test
    fun initForceTemplateDoesNotOverwriteLinkedRepoConfig() {
        writeRepoConfig("""{ "dots": { "zsh": { "source": "~/.zshrc", "target": "~/dotfiles/zshrc" } } }""")
        assertEquals(0, init("$repoConfig").statusCode)
        assertEquals(0, init("--force").statusCode)
        assertEquals(null, fs.metadataOrNull(configPath)?.symlinkTarget)
        assertContains(fs.read(repoConfig) { readUtf8() }, "zsh")
    }

    @Test
    fun initLinkRejectsMissingOrInvalidFile() {
        assertEquals(1, init("$repoConfig").statusCode)
        writeRepoConfig("{ not json")
        assertEquals(1, init("$repoConfig").statusCode)
        assertEquals(null, fs.metadataOrNull(configPath))
    }

    private fun config(vararg dots: Pair<String, Entry>) = Config(dots = mapOf(*dots))
}
