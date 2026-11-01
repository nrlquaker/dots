package commands

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import config.ResolvedEntry
import config.config

class Backup(env: Env) : BaseCommand(env) {
    private val overwrite by option("-o", "--overwrite", help = "Replace an existing target")
        .flag("--no-overwrite", default = env.loadResult.config?.backup?.overwrite ?: false)

    override fun plan(entry: ResolvedEntry) = operations.planBackup(entry, overwrite)

    override fun help(context: Context): String {
        return "Move dotfiles to their targets and leave symlinks behind. Each source is moved to its target " +
                "and a symbolic link pointing to the target is created at the source location."
    }
}
