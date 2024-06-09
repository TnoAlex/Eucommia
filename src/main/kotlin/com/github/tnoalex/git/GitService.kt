package com.github.tnoalex.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter
import java.io.File
import java.io.OutputStream

class GitService(gitRepo: File) : AutoCloseable {
    private val git = Git.open(gitRepo)
    private val repository = git.repository
    val repoName: String = gitRepo.name
    val repoPath: String = gitRepo.canonicalPath

    fun getDiffFormatter(outputStream: OutputStream, context: Int): DiffFormatter {
        return DiffFormatter(outputStream).also { f -> f.setRepository(repository);f.setContext(context) }
    }

    private fun useRevWalk(walker: (RevWalk) -> Unit) {
        RevWalk(repository).use(walker)
    }

    fun visitCommit(ref: String?, filter: RevFilter, visitor: (RevCommit) -> Unit) {
        val visitRef = getRef(ref)
        useRevWalk { walk ->
            visitCommit(walk, visitRef, filter, visitor)
        }
    }

    private fun getRef(ref: String?): Ref {
        return ref?.let { repository.findRef(ref) } ?: repository.refDatabase.refs.first()
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
        diffCallBack: (DiffEntry, ObjectReader) -> Unit
    ) {
        if (commit.parentCount <= 0) return
        visitDiff(commit.getParent(0), commit, pathFilter, typeFilters, diffCallBack)
    }

    fun visitDiff(
        oldCommit: RevCommit,
        newCommit: RevCommit,
        pathFilter: TreeFilter?,
        typeFilters: List<DiffEntry.ChangeType>,
        diffCallBack: (DiffEntry, ObjectReader) -> Unit
    ) {
        val newTree = CanonicalTreeParser()
        val oldTree = CanonicalTreeParser()
        val objectReader = repository.newObjectReader()
        objectReader.use { reader ->
            newTree.reset(reader, newCommit.tree.id)
            oldTree.reset(reader, oldCommit.tree.id)

            val diffs = git.diff()
                .setNewTree(newTree)
                .setOldTree(oldTree)
                .setPathFilter(pathFilter)
                .call()
                .filter { it.changeType in typeFilters }

            diffs.forEach { diff ->
                diffCallBack(diff, reader)
            }
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

    fun visitLogs(logConfigurator: (LogCommand) -> LogCommand, visitor: (RevCommit) -> Unit) {
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