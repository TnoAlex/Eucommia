package com.github.tnoalex

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader
import java.io.Reader

class GitCommitsVisitor(
    gitRepoPath: File,
    private val mainRef: String?,
    private val commitCallback: (String, String, Reader, Reader) -> Unit
) {
    private val git: Git = Git.open(gitRepoPath)
    private var repository: Repository = git.repository


    fun visitGitRepo() {
        repository.use { rep ->
            val mainRef = rep.findRef(mainRef) ?: rep.findRef("refs/heads/main") ?: rep.findRef("refs/heads/master")
            if (mainRef == null) {
                logger.error("Can not find git ref in ${rep.directory.path}")
            }
            RevWalk(rep).use { walk ->
                visitCommits(walk, mainRef)
            }
        }
    }

    private fun visitCommits(revWalk: RevWalk, branch: Ref) {
        val startCommit = revWalk.parseCommit(branch.objectId)
        revWalk.markStart(startCommit)
        revWalk.revFilter = RevFilter.NO_MERGES
        revWalk.forEach {
            visitDiffs(it)
        }
    }

    private fun visitDiffs(commit: RevCommit) {
        val newTree = CanonicalTreeParser()
        val oldTree = CanonicalTreeParser()
        val objectReader = repository.newObjectReader()
        objectReader.use { reader ->
            if (commit.parentCount <= 0) return
            newTree.reset(reader, commit.tree.id)
            oldTree.reset(reader, commit.getParent(0).tree.id)

            val diffs = git.diff()
                .setNewTree(newTree)
                .setOldTree(oldTree)
                .setPathFilter(OR_PATH_FILTER)
                .call()
                .filter { it.changeType == DiffEntry.ChangeType.MODIFY }

            diffs.forEach { diff ->
                val newLoader = reader.open(diff.newId.toObjectId())
                val oldLoader = reader.open(diff.oldId.toObjectId())
                val newContentReader = InputStreamReader(newLoader.openStream())
                val oldContentReader = InputStreamReader(oldLoader.openStream())
                commitCallback(commit.id.name, diff.newPath.toString(), oldContentReader, newContentReader)
            }
        }
    }

    companion object {
        private val OR_PATH_FILTER =
            OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))
        private val logger = LoggerFactory.getLogger(GitCommitsVisitor::class.java)
    }
}