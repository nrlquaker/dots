import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import commands.Backup
import commands.List
import commands.Sync
import commands.Restore

fun main(args: Array<String>) {
    Dots()
        .subcommands(Backup(), Restore(), Sync(), List())
        .context { terminal = Terminal(theme = Theme.Plain) }
        .main(args)
}
