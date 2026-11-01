import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.versionOption

class Dots: CliktCommand() {
    init {
        versionOption(VERSION)
    }

    override fun run() = Unit

    override fun help(context: Context): String {
        return "A tool for syncing dotfiles. dots is a lightweight, command-line utility designed to simplify\n" +
                "the management and synchronization of your dotfiles across multiple systems"
    }
}
