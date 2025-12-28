package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.sources.ValueSource

class Sync: CliktCommand() {
    init {
        context {
//            valueSources(
//                JsonValueSource.from(System.getProperty("user.dir") + "/config.json"),
//                JsonValueSource.from(System.getProperty("user.dir") + "/samples/json/config.json")
//            )
        }
    }

    init {
//        context {
//            valueSource = PropertiesValueSource.from("myconfig.properties")
//        }
    }

    val overwrite: Boolean by option("-o", "--overwrite", help = "enable verbose mode").flag(default = false)

    override fun run() {
//        ValueSource
    }

    override fun help(context: Context): String {
        return "Sync"
    }
}