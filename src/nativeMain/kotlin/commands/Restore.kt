package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Restore: CliktCommand() {
    val overwrite: Boolean by option("-o", "--overwrite", help = "enable verbose mode").flag(default = false)

    override fun run() {

    }

    override fun help(context: Context): String {
        return "Remove symlinks for dotfiles. Restore original files by removing the symlinks created by the" +
                "'link' command. This will remove the symbolic links at the source locations and move the files" +
                "from target back to source."
    }
}
