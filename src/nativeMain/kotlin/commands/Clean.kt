package commands

import com.github.ajalt.clikt.core.Context
import config.ResolvedEntry

class Clean(env: Env) : BaseCommand(env) {
    override fun plan(entry: ResolvedEntry) = operations.planClean(entry)

    override fun help(context: Context): String {
        return "Remove broken symlinks managed by dots. Only a source that links to its configured target " +
                "while the target is missing is removed. Other links are never touched."
    }
}
