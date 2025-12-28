package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int

class List: CliktCommand() {
    val v: Boolean by option("-v", "--verbose", help = "enable verbose mode").flag(default = true)
    val count: Int by option().int().default(1).help("Number of greetings")
    val name: String by option().prompt("Your name").help("The person to greet")

    override fun run() {
        repeat(count) {
            echo("Hello $name!")
            echo("Verbose: $v")
        }
    }

    override fun help(context: Context): String {
        return "List all tracked dotfiles. Display all tracked dotfiles and their target locations." +
                "This command shows a list of all dotfiles in the configuration file," +
                "including their source and target paths."
    }
}
