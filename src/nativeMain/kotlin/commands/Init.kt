package commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import config.CONFIG_TEMPLATE
import config.ConfigLoader
import config.LoadResult
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

class Init(private val env: Env) : CliktCommand() {
    private val from by argument(help = "Existing config to link to ~/.config/dots/config").optional()
    private val force by option("-f", "--force", help = "Overwrite an existing config").flag()

    override fun run() {
        val path = ConfigLoader.configPath(env.home)
        val linkTo = from?.let(::resolveConfig)
        if (env.fs.metadataOrNull(path) != null) {
            if (env.loadResult !is LoadResult.Missing && !force) {
                throw CliktError("Config already exists at ${env.display(path)} (use --force to overwrite it)")
            }
            env.fs.delete(path)
        }
        path.parent?.let { env.fs.createDirectories(it) }
        if (linkTo == null) {
            env.fs.write(path) { writeUtf8(CONFIG_TEMPLATE) }
            echo("Created ${env.display(path)}. Add your entries under \"dots\".")
        } else {
            env.fs.createSymlink(path, linkTo)
            echo("Linked ${env.display(path)} -> ${env.display(linkTo)}. Run `dots sync` to link the rest.")
        }
    }

    private fun resolveConfig(raw: String): Path {
        val file = try {
            env.fs.canonicalize(ConfigLoader.expand(raw, env.home) ?: raw.toPath())
        } catch (_: IOException) {
            throw CliktError("${ConfigLoader.expand(raw, env.home)?.let(env::display) ?: raw} does not exist")
        }
        if (file == ConfigLoader.configPath(env.home)) {
            throw CliktError("${env.display(file)} is already the config location")
        }
        if (ConfigLoader.loadFrom(env.fs, file) !is LoadResult.Loaded) {
            throw CliktError("Config ${env.display(file)} is malformed, nothing was linked")
        }
        return file
    }

    override fun help(context: Context): String {
        return "Create a template config at ~/.config/dots/config, or link an existing config file there. " +
                "On a new machine, run `dots init <path-to-config-in-your-dotfiles>` and then `dots sync`."
    }
}
