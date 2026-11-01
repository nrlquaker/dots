package config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

sealed interface LoadResult {
    data class Loaded(val config: Config) : LoadResult
    data class Missing(val path: Path) : LoadResult
    data class Invalid(val path: Path) : LoadResult
}

val LoadResult.config: Config?
    get() = (this as? LoadResult.Loaded)?.config

class InvalidEntriesException(val errors: List<String>) : Exception(errors.joinToString("\n"))

object ConfigLoader {
    fun configPath(home: Path): Path = home / ".config" / "dots" / "config"

    fun load(fs: FileSystem, home: Path): LoadResult = loadFrom(fs, configPath(home))

    fun loadFrom(fs: FileSystem, path: Path): LoadResult {
        val link = fs.metadataOrNull(path)?.symlinkTarget
        val file = if (link == null) path else path.parent!! / link
        if (!fs.exists(file)) return LoadResult.Missing(path)
        return try {
            LoadResult.Loaded(Json.decodeFromString<Config>(fs.read(path) { readUtf8() }))
        } catch (_: SerializationException) {
            LoadResult.Invalid(path)
        } catch (_: IOException) {
            LoadResult.Invalid(path)
        }
    }

    fun resolveEntries(config: Config, home: Path): List<ResolvedEntry> {
        val errors = mutableListOf<String>()
        val entries = config.dots.mapNotNull { (name, entry) ->
            val source = expand(entry.source, home)
            val target = expand(entry.target, home)
            if (source == null) errors += "$name: source '${entry.source}' must be absolute or start with ~"
            if (target == null) errors += "$name: target '${entry.target}' must be absolute or start with ~"
            if (source == null || target == null) return@mapNotNull null
            if (source == target) errors += "$name: source and target are the same path"
            ResolvedEntry(name, source, target)
        }
        entries.groupBy { it.source }.filterValues { it.size > 1 }.forEach { (path, dupes) ->
            errors += "entries ${dupes.joinToString { it.name }} share the source $path"
        }
        entries.groupBy { it.target }.filterValues { it.size > 1 }.forEach { (path, dupes) ->
            errors += "entries ${dupes.joinToString { it.name }} share the target $path"
        }
        if (errors.isNotEmpty()) throw InvalidEntriesException(errors)
        return entries
    }

    internal fun expand(raw: String, home: Path): Path? {
        val path = when {
            raw == "~" -> home
            raw.startsWith("~/") -> home / raw.removePrefix("~/")
            else -> raw.toPath()
        }.normalized()
        return path.takeIf { it.isAbsolute }
    }
}
