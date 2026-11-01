package config

import kotlinx.serialization.Serializable
import okio.Path

@Serializable
data class Config(
    val backup: CommandOptions? = null,
    val restore: CommandOptions? = null,
    val sync: CommandOptions? = null,
    val dots: Map<String, Entry>,
)

@Serializable
data class CommandOptions(
    val overwrite: Boolean? = null,
)

@Serializable
data class Entry(val source: String, val target: String)

data class ResolvedEntry(val name: String, val source: Path, val target: Path)
