package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import fs.Operations
import fs.Status

class ListCommand(private val env: Env) : CliktCommand(name = "list") {
    override fun run() {
        val entries = env.requireEntries()
        if (entries.isEmpty()) {
            echo("No entries in config. Add them under \"dots\".")
            return
        }
        val operations = Operations(env.fs, env::display)
        val nameWidth = entries.maxOf { it.name.length }
        val paths = entries.associateWith { "${env.display(it.source)} -> ${env.display(it.target)}" }
        val pathsWidth = paths.values.maxOf { it.length }
        entries.forEach {
            val status = operations.status(it)
            val mark = if (status == Status.LINKED) BaseCommand.CHECK else BaseCommand.CROSS
            echo("$mark ${it.name.padEnd(nameWidth)}  ${paths.getValue(it).padEnd(pathsWidth)}   ${status.label}")
        }
    }

    override fun help(context: Context): String {
        return "List all tracked dotfiles with their source, target and current status."
    }
}
