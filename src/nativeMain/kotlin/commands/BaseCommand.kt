package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import config.ResolvedEntry
import fs.Action
import fs.Operations
import fs.PlanResult
import okio.IOException

abstract class BaseCommand(protected val env: Env) : CliktCommand() {
    protected val operations = Operations(env.fs, env::display)

    protected abstract fun plan(entry: ResolvedEntry): PlanResult

    override fun run() {
        val plans = env.requireEntries().map { it to plan(it) }
        val title = commandName.replaceFirstChar { it.uppercase() }
        val nameWidth = plans.maxOfOrNull { it.first.name.length } ?: 0
        val pathsWidth = plans.maxOfOrNull { paths(it.first).length } ?: 0

        fun line(mark: String, entry: ResolvedEntry, note: String) =
            echo("$mark ${entry.name.padEnd(nameWidth)}  ${paths(entry).padEnd(pathsWidth)}   $note".trimEnd())

        echo("dots: $commandName")
        echo()

        if (plans.any { it.second is PlanResult.Error }) {
            for ((entry, result) in plans) when (result) {
                is PlanResult.Error -> line(CROSS, entry, result.message)
                is PlanResult.Actions -> line(DOT, entry, if (result.actions.isEmpty()) result.note else "skipped")
            }
            echo()
            echo("===> $title failed: nothing was changed")
            throw ProgramResult(1)
        }

        var changed = 0
        var unchanged = 0
        for ((entry, result) in plans) {
            result as PlanResult.Actions
            if (result.actions.isEmpty()) {
                unchanged++
                line(DOT, entry, result.note)
                continue
            }
            for (action in result.actions) {
                try {
                    operations.execute(action)
                } catch (e: IOException) {
                    line(CROSS, entry, "failed to ${describe(action)}: ${e.message}")
                    echo()
                    echo("===> $title failed: $changed ${entries(changed)} completed, ${entry.name} may be partially applied")
                    throw ProgramResult(1)
                }
            }
            changed++
            line(CHECK, entry, result.note)
        }
        echo()
        echo("===> $title successful: $changed changed, $unchanged unchanged")
    }

    private fun paths(entry: ResolvedEntry) = "${env.display(entry.source)} -> ${env.display(entry.target)}"

    private fun entries(count: Int) = if (count == 1) "entry" else "entries"

    private fun describe(action: Action): String = when (action) {
        is Action.MkDirs -> "create directory ${env.display(action.path)}"
        is Action.Move -> "move ${env.display(action.from)} to ${env.display(action.to)}"
        is Action.Delete -> "delete ${env.display(action.path)}"
        is Action.Symlink -> "link ${env.display(action.at)} to ${env.display(action.to)}"
        is Action.RemoveLink -> "remove link ${env.display(action.path)}"
    }

    internal companion object {
        const val CHECK = "✓"
        const val CROSS = "✗"
        const val DOT = "•"
    }
}
