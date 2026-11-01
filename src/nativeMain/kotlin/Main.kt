import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import commands.Backup
import commands.Clean
import commands.Env
import commands.Init
import commands.ListCommand
import commands.Restore
import commands.Sync
import config.ConfigLoader
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.system.exitProcess

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    val home = getenv("HOME")?.toKString()?.takeIf { it.isNotEmpty() }?.toPath() ?: run {
        println("HOME is not set")
        exitProcess(1)
    }
    val fs = FileSystem.SYSTEM
    val env = Env(fs, home, ConfigLoader.load(fs, home))

    Dots()
        .subcommands(Init(env), Backup(env), Restore(env), Sync(env), Clean(env), ListCommand(env))
        .context {
            terminal = Terminal(theme = Theme.Plain)
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
        }
        .main(args)
}
