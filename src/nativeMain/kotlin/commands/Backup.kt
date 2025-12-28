package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class Backup: CliktCommand() {
    val overwrite: Boolean by option("-o", "--overwrite", help = "enable verbose mode").flag(default = false)

    override fun run() {

    }

    override fun help(context: Context): String {
        return "Create symlinks for dotfiles. Create symlinks for all specified dotfiles based on the configuration" +
                "file. This will move the actual files from source to target locations and create symbolic links" +
                "at the source locations that point to the target files."
    }
}
