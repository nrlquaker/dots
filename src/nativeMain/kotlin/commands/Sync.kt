package commands

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import config.ResolvedEntry
import config.config

class Sync(env: Env) : BaseCommand(env) {
    private val overwrite by option("-o", "--overwrite", help = "Replace an existing file or link at the source")
        .flag("--no-overwrite", default = env.loadResult.config?.sync?.overwrite ?: false)

    override fun plan(entry: ResolvedEntry) = operations.planSync(entry, overwrite)

    override fun help(context: Context): String {
        return "Create missing symlinks. For each entry whose target exists, creates or fixes the symbolic " +
                "link at the source. Nothing is moved, which makes it the command for setting up a new machine."
    }
}
