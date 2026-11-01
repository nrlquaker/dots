package fs

import config.ResolvedEntry
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import okio.FileSystem
import okio.IOException
import okio.Path
import platform.posix.chmod
import platform.posix.lstat
import platform.posix.stat

sealed interface Action {
    data class MkDirs(val path: Path) : Action
    data class Move(val from: Path, val to: Path) : Action
    data class Delete(val path: Path) : Action
    data class Symlink(val at: Path, val to: Path) : Action
    data class RemoveLink(val path: Path) : Action
}

sealed interface PlanResult {
    data class Actions(val actions: List<Action>, val note: String) : PlanResult
    data class Error(val message: String) : PlanResult
}

enum class Status(val label: String) {
    LINKED("linked"),
    NOT_LINKED("not linked"),
    NOT_BACKED_UP("not backed up"),
    BROKEN("broken"),
    CONFLICT("conflict"),
    MISSING("missing"),
}

class Operations(private val fs: FileSystem, private val display: (Path) -> String) {

    fun planBackup(entry: ResolvedEntry, overwrite: Boolean): PlanResult {
        val (_, source, target) = entry
        if (isLinkTo(source, target)) {
            return if (exists(target)) PlanResult.Actions(emptyList(), "already backed up")
            else PlanResult.Error("source is a broken link to the missing target, run `dots clean`")
        }
        if (!exists(source)) return PlanResult.Error("source does not exist")
        linkTarget(source)?.let { return PlanResult.Error("source is a symlink to ${display(it)}, not a file or directory") }

        val actions = mutableListOf<Action>()
        var note = "moved to target and linked"
        if (exists(target)) {
            if (!overwrite) return PlanResult.Error("target already exists (use --overwrite to replace it)")
            actions += Action.Delete(target)
            note = "replaced existing target, moved and linked"
        } else {
            target.parent?.takeIf { !exists(it) }?.let { actions += Action.MkDirs(it) }
        }
        actions += Action.Move(source, target)
        actions += Action.Symlink(source, target)
        return PlanResult.Actions(actions, note)
    }

    fun planRestore(entry: ResolvedEntry, overwrite: Boolean): PlanResult {
        val (_, source, target) = entry
        if (!exists(target)) return PlanResult.Error("target does not exist")

        val actions = mutableListOf<Action>()
        val note = when {
            isLinkTo(source, target) -> {
                actions += Action.RemoveLink(source)
                "removed link and moved back"
            }
            !exists(source) -> {
                source.parent?.takeIf { !exists(it) }?.let { actions += Action.MkDirs(it) }
                "moved back"
            }
            !overwrite -> return PlanResult.Error("source already exists (use --overwrite to replace it)")
            else -> {
                actions += Action.Delete(source)
                "replaced existing source and moved back"
            }
        }
        actions += Action.Move(target, source)
        return PlanResult.Actions(actions, note)
    }

    fun planSync(entry: ResolvedEntry, overwrite: Boolean): PlanResult {
        val (_, source, target) = entry
        if (!exists(target)) return PlanResult.Error("target does not exist")
        if (isLinkTo(source, target)) return PlanResult.Actions(emptyList(), "already linked")

        val actions = mutableListOf<Action>()
        val note = when {
            !exists(source) -> {
                source.parent?.takeIf { !exists(it) }?.let { actions += Action.MkDirs(it) }
                "linked"
            }
            !overwrite -> return PlanResult.Error("source already exists (use --overwrite to replace it)")
            else -> {
                actions += Action.Delete(source)
                "replaced existing source with link"
            }
        }
        actions += Action.Symlink(source, target)
        return PlanResult.Actions(actions, note)
    }

    fun planClean(entry: ResolvedEntry): PlanResult {
        val (_, source, target) = entry
        return if (isLinkTo(source, target) && !exists(target)) PlanResult.Actions(listOf(Action.RemoveLink(source)), "removed broken link")
        else PlanResult.Actions(emptyList(), "nothing to clean")
    }

    fun status(entry: ResolvedEntry): Status {
        val (_, source, target) = entry
        val link = linkTarget(source)
        return when {
            link == target -> if (exists(target)) Status.LINKED else Status.BROKEN
            link != null -> Status.CONFLICT
            exists(source) -> if (exists(target)) Status.CONFLICT else Status.NOT_BACKED_UP
            exists(target) -> Status.NOT_LINKED
            else -> Status.MISSING
        }
    }

    fun execute(action: Action) {
        when (action) {
            is Action.MkDirs -> fs.createDirectories(action.path)
            is Action.Move -> move(action.from, action.to)
            is Action.Delete -> delete(action.path)
            is Action.Symlink -> fs.createSymlink(action.at, action.to)
            is Action.RemoveLink -> fs.delete(action.path)
        }
    }

    private fun exists(path: Path) = fs.metadataOrNull(path) != null

    private fun linkTarget(path: Path): Path? {
        val link = fs.metadataOrNull(path)?.symlinkTarget ?: return null
        return if (link.isAbsolute) link.normalized() else (path.parent!! / link).normalized()
    }

    private fun isLinkTo(path: Path, target: Path) = linkTarget(path) == target

    private fun delete(path: Path) {
        if (linkTarget(path) != null) fs.delete(path) else fs.deleteRecursively(path)
    }

    private fun move(from: Path, to: Path) {
        try {
            fs.atomicMove(from, to)
        } catch (_: IOException) {
            copyRecursively(from, to)
            delete(from)
        }
    }

    private fun copyRecursively(from: Path, to: Path) {
        val metadata = fs.metadata(from)
        val link = metadata.symlinkTarget
        when {
            link != null -> fs.createSymlink(to, link)
            metadata.isDirectory -> {
                fs.createDirectory(to)
                fs.list(from).forEach { copyRecursively(it, to / it.name) }
            }
            else -> fs.copy(from, to)
        }
        if (link == null) copyMode(from, to)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun copyMode(from: Path, to: Path) {
        if (fs !== FileSystem.SYSTEM) return
        memScoped {
            val st = alloc<stat>()
            if (lstat(from.toString(), st.ptr) == 0) chmod(to.toString(), st.st_mode)
        }
    }
}
