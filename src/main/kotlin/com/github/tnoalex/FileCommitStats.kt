package com.github.tnoalex

import com.github.tnoalex.util.ifFalse
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private var git: Git? = null
private var statsOutputStream = FileOutputStream.nullOutputStream()
private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))
private var repo: Repository? = null
private val logger = LoggerFactory.getLogger("FileCommitStats")

fun stats(gitRepo: File, storePath: File) {
    git = Git.open(gitRepo)
    repo = git!!.repository
    storePath.isDirectory.ifFalse { return }
    storePath.exists().ifFalse {
        storePath.mkdirs()
    }
    statsOutputStream =
        FileOutputStream(
            storePath.resolve("${gitRepo.name}.csv").also { it.exists().ifFalse { it.createNewFile() } },
            true
        )
    repo.use reposUse@{
        val treeWalk = TreeWalk(it)
        val mainHead =
            it!!.findRef("refs/heads/main") ?: it.findRef("refs/heads/master") ?: it.findRef("refs/heads/1.x")
        if (mainHead == null) {
            logger.info("${gitRepo.name} can not find refs/heads/main or refs/heads/master")
            return@reposUse
        }
        val revWalk = RevWalk(it)
        val tree = revWalk.parseCommit(mainHead.objectId).tree
        val repoStats = ArrayList<List<Any>>()
        treeWalk.use { walk ->
            walk.addTree(tree)
            walk.isRecursive = true
            walk.filter = OR_PATH_FILTER
            while (walk.next()) {
                val logs = git!!.log().addPath(walk.pathString)
                    .setRevFilter(RevFilter.NO_MERGES)
                    .all()
                    .call()
                var modifierCount = 0L
                var addLoc = 0L
                var delLoc = 0L
                logs.forEach { log ->
                    val (flag, add, del) = diffVisit(log, walk.pathString) ?: return@forEach
                    if (flag) modifierCount++
                    addLoc += add
                    delLoc += del
                }
                if (listOf(modifierCount, addLoc, delLoc).any { t -> t != 0L }) {
                    repoStats.add(listOf(walk.pathString, modifierCount, addLoc, delLoc))
                }
            }
        }
        writeCsv(repoStats)
    }
    statsOutputStream.write(System.lineSeparator().toByteArray())
    git!!.close()
    statsOutputStream.close().also {
        statsOutputStream = OutputStream.nullOutputStream()
    }
}

private fun diffVisit(commit: RevCommit, newPath: String): Triple<Boolean, Long, Long>? {
    if (commit.parentCount <= 0) return null
    val outputStream = ByteArrayOutputStream()
    val diffFormatter = DiffFormatter(outputStream).also { f -> f.setRepository(repo!!);f.setContext(0) }
    var addLoc = 0L
    var delLoc = 0L
    var modifierFlag = false
    diffsFormCommit(commit, newPath).forEach {
        modifierFlag = true
        diffFormatter.format(it)
        val formattedDiff = outputStream.toString().split(Regex("@@.*?@@")).last()
        modifierStats(formattedDiff).also { s ->
            addLoc += s.first
            delLoc += s.second
        }
        outputStream.reset()
    }
    return Triple(modifierFlag, addLoc, delLoc)
}

private fun modifierStats(text: String): Pair<Long, Long> {
    var addLoc = 0L
    var delLoc = 0L
    text.replace("\r\n", "\n").split("\n").forEach {
        if (it.isBlank()) return@forEach
        if (it.trimStart().startsWith("+")) addLoc++
        else if (it.trimStart().startsWith("-")) delLoc++
    }
    return Pair(addLoc, delLoc)
}

private fun diffsFormCommit(commit: RevCommit, newPath: String): List<DiffEntry> {
    val newTree = CanonicalTreeParser()
    val oldTree = CanonicalTreeParser()
    val reader = repo!!.newObjectReader()
    reader.use {
        newTree.reset(reader, commit.tree.id)
        oldTree.reset(reader, commit.getParent(0).tree.id)
        val diffs = git!!.diff()
            .setNewTree(newTree)
            .setOldTree(oldTree)
            .call()
            .filter { it.changeType == DiffEntry.ChangeType.MODIFY }
            .filter { it.newPath == newPath }
        return diffs
    }
}

private fun writeCsv(result: ArrayList<List<Any>>) {
    val sb = StringBuilder("path,modifierCount,addLocs,delLocs\n")
    result.forEach {
        sb.append(it.joinToString(",")).append("\n")
    }
    statsOutputStream.write(sb.toString().toByteArray())
}