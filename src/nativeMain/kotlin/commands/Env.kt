package commands

import com.github.ajalt.clikt.core.CliktError
import config.ConfigLoader
import config.InvalidEntriesException
import config.LoadResult
import config.ResolvedEntry
import okio.FileSystem
import okio.Path

class Env(val fs: FileSystem, val home: Path, val loadResult: LoadResult) {

    fun requireEntries(): List<ResolvedEntry> = when (loadResult) {
        is LoadResult.Missing -> throw CliktError(
            "Config not found at ${display(loadResult.path)}. Run `dots init` to create one, or `dots init <path>` to link an existing config."
        )
        is LoadResult.Invalid -> throw CliktError(
            "Config at ${display(loadResult.path)} is malformed. Fix it or run `dots init --force` to start over."
        )
        is LoadResult.Loaded -> try {
            ConfigLoader.resolveEntries(loadResult.config, home)
        } catch (e: InvalidEntriesException) {
            throw CliktError("Invalid config ${display(ConfigLoader.configPath(home))}:\n${e.message}")
        }
    }

    fun display(path: Path): String =
        if (path == home) "~"
        else path.relativeToOrNull(home)?.takeIf { !it.toString().startsWith("..") }?.let { "~/$it" }
            ?: path.toString()
}

private fun Path.relativeToOrNull(other: Path): Path? = try {
    relativeTo(other)
} catch (_: IllegalArgumentException) {
    null
}
