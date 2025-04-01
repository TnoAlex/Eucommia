package com.github.tnoalex.git

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.eclipse.jgit.util.io.NullOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream


class GitService(gitRepo: File) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val git = Git.open(gitRepo)
    private val repository = git.repository
    val repoName: String = gitRepo.name
    val repoPath: String = gitRepo.canonicalPath

    fun getDiffFormatter(outputStream: OutputStream, context: Int): DiffFormatter {
        return DiffFormatter(outputStream).also { f -> f.setRepository(repository);f.setContext(context) }
    }

    private inline fun useRevWalk(walker: (RevWalk) -> Unit) {
        RevWalk(repository).use(walker)
    }

    fun visitCommit(ref: String?, filter: RevFilter, visitor: (RevCommit) -> Unit) {
        val visitRef = getRef(ref)
        useRevWalk { walk ->
            visitCommit(walk, visitRef, filter, visitor)
        }
    }

    suspend fun visitCommitAsync(ref: String?, filter: RevFilter, commitChannel: Channel<RevCommit>) {
        val visitRef = getRef(ref)
        RevWalk(repository).use { walk ->
            val startCommit = walk.parseCommit(visitRef.objectId)
            walk.markStart(startCommit)
            walk.revFilter = filter
            for (it in walk) {
                commitChannel.send(it)
                logger.trace { "send commit: ${it.id.name} to channel" }
            }
        }
        commitChannel.close()
    }

    private fun getRef(ref: String?): Ref {
        return ref?.let { repository.findRef(ref) }
            ?: repository.findRef("master")
            ?: repository.findRef("main")
            ?: repository.refDatabase.refs.first()
            ?: throw RuntimeException("Can not find ref $ref")
    }

    private fun visitCommit(walk: RevWalk, ref: Ref, filter: RevFilter, visitor: (RevCommit) -> Unit) {
        val startCommit = walk.parseCommit(ref.objectId)
        walk.markStart(startCommit)
        walk.revFilter = filter
        walk.forEach {
            visitor(it)
        }
    }

    fun visitDiffWithParent(
        commit: RevCommit,
        pathFilter: TreeFilter?,
        typeFilters: List<DiffEntry.ChangeType>,
        diffsFilter: (List<DiffEntry>) -> Boolean = { _ -> true },
        diffCallBack: (DiffEntry, ObjectReader) -> Unit
    ) {
        if (commit.parentCount <= 0) return
        visitDiff(commit.getParent(0), commit, pathFilter, typeFilters, diffsFilter, diffCallBack)
    }

    suspend fun visitDiffWithParentAsync(
        commit: RevCommit,
        pathFilter: TreeFilter?,
        typeFilters: List<DiffEntry.ChangeType>,
        diffsFilter: (List<DiffEntry>) -> Boolean = { _ -> true },
        diffCallBack: suspend (DiffEntry, ObjectReader) -> Unit
    ) {
        if (commit.parentCount <= 0) return
        visitDiffAsync(commit.getParent(0), commit, pathFilter, typeFilters, diffsFilter, diffCallBack)
    }

    fun visitDiff(
        oldCommit: RevCommit,
        newCommit: RevCommit,
        pathFilter: TreeFilter?,
        typeFilters: List<DiffEntry.ChangeType>,
        diffsFilter: (List<DiffEntry>) -> Boolean = { _ -> true },
        diffCallBack: (DiffEntry, ObjectReader) -> Unit
    ) {
        val newTree = CanonicalTreeParser()
        val oldTree = CanonicalTreeParser()
        val objectReader = repository.newObjectReader()
        try {
            objectReader.use { reader ->
                newTree.reset(reader, newCommit.tree.id)
                oldCommit.tree?.let { oldTree.reset(reader, it.id) } ?: return
                DiffFormatter(NullOutputStream.INSTANCE).use { diffFmt ->
                    diffFmt.setRepository(repository)
                    diffFmt.pathFilter = pathFilter
                    diffFmt.isDetectRenames = true

                    val diffs = diffFmt.scan(oldTree, newTree)
                    diffFmt.format(diffs)
                    diffFmt.flush()
                    if (diffsFilter(diffs.filter { it.changeType in typeFilters })) {
                        diffs.forEach { diff ->
                            diffCallBack(diff, reader)
                        }
                    } else {
                        logger.info { "ignore ${newCommit.id.name}" }
                    }
                }
            }
        } catch (e: IOException) {
            throw JGitInternalException(e.message, e)
        }
    }

    suspend fun visitDiffAsync(
        oldCommit: RevCommit,
        newCommit: RevCommit,
        pathFilter: TreeFilter?,
        typeFilters: List<DiffEntry.ChangeType>,
        diffsFilter: (List<DiffEntry>) -> Boolean = { _ -> true },
        diffCallBack: suspend (DiffEntry, ObjectReader) -> Unit
    ) {
        val newTree = CanonicalTreeParser()
        val oldTree = CanonicalTreeParser()
        val objectReader = repository.newObjectReader()
        objectReader.use { reader ->
            newTree.reset(reader, newCommit.tree.id)
            oldCommit.tree?.let { oldTree.reset(reader, it.id) } ?: return
        }
        val diffs = git.diff()
            .setNewTree(newTree)
            .setOldTree(oldTree)
            .setPathFilter(pathFilter)
            .call()
            .filter { it.changeType in typeFilters }

        if (diffsFilter(diffs)) {
            diffs.forEach { diff ->
                withContext(Dispatchers.IO) {
                    launch {
                        repository.newObjectReader().use {
                            diffCallBack(diff, it)
                        }
                    }
                }
            }
        } else {
            logger.info { "ignore ${newCommit.id.name}" }
        }
    }

    fun visitTree(ref: String?, treeFilter: TreeFilter?, isRecursive: Boolean, visitor: (TreeWalk) -> Unit) {
        val treeWalk = TreeWalk(repository)
        useRevWalk { revWalk ->
            treeWalk.use { walk ->
                walk.addTree(revWalk.parseCommit(getRef(ref).objectId).tree)
                walk.isRecursive = isRecursive
                walk.filter = treeFilter
                while (walk.next()) {
                    visitor(walk)
                }
            }
        }
    }

    suspend fun visitTreeAsync(
        ref: String?, treeFilter: TreeFilter?, isRecursive: Boolean,
        visitor: suspend (TreeWalk) -> Unit
    ) {
        val treeWalk = TreeWalk(repository)
        useRevWalk { revWalk ->
            treeWalk.use { walk ->
                walk.addTree(revWalk.parseCommit(getRef(ref).objectId).tree)
                walk.isRecursive = isRecursive
                walk.filter = treeFilter
                while (walk.next()) {
                    visitor(walk)
                }
            }
        }
    }

    fun visitLogs(logConfigurator: (LogCommand) -> LogCommand, visitor: (RevCommit) -> Unit) {
        logConfigurator(git.log()).call().forEach {
            visitor(it)
        }
    }

    suspend fun visitLogsAsync(
        logConfigurator: suspend (LogCommand) -> LogCommand,
        visitor: suspend (RevCommit) -> Unit
    ) {
        logConfigurator(git.log()).call().forEach {
            visitor(it)
        }
    }

    fun parseCommit(objectId: String): RevCommit? {
        return repository.parseCommit(ObjectId.fromString(objectId))
    }

    override fun close() {
        repository.close()
        git.close()
    }
}