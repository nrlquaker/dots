package commands

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import config.ResolvedEntry
import config.config

class Restore(env: Env) : BaseCommand(env) {
    private val overwrite by option("-o", "--overwrite", help = "Replace an existing file at the source")
        .flag("--no-overwrite", default = env.loadResult.config?.restore?.overwrite ?: false)

    override fun plan(entry: ResolvedEntry) = operations.planRestore(entry, overwrite)

    override fun help(context: Context): String {
        return "Undo backup. Removes the symbolic link at each source and moves the file from its target " +
                "back to the source location."
    }
}
