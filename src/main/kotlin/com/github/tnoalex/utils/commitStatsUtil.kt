package com.github.tnoalex.utils

import com.github.tnoalex.git.GitService
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import java.io.ByteArrayOutputStream
import kotlin.reflect.KFunction

private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))

fun statsCommit(gitService: GitService, mainRef: String?): String {
    var modifierCount = 0L
    var addLoc = 0L
    var delLoc = 0L
    val repoStats = ArrayList<List<Any>>()
    gitService.visitTree(mainRef, OR_PATH_FILTER, true) { treeWalk ->
        gitService.visitLogs({ logCommand ->
            logCommand.addPath(treeWalk.pathString)
                .setRevFilter(RevFilter.NO_MERGES)
                .all()
        }) { log ->
            val (flag, add, del) = diffVisit(log, treeWalk.pathString, gitService) ?: return@visitLogs
            if (flag) modifierCount++
            addLoc += add
            delLoc += del
            if (listOf(modifierCount, addLoc, delLoc).any { t -> t != 0L }) {
                repoStats.add(listOf(treeWalk.pathString, modifierCount, addLoc, delLoc))
            }
        }

    }
    return writeCsv(repoStats)
}

private fun diffVisit(commit: RevCommit, newPath: String, gitService: GitService): Triple<Boolean, Long, Long>? {
    if (commit.parentCount <= 0) return null
    val outputStream = ByteArrayOutputStream()
    val diffFormatter = gitService.getDiffFormatter(outputStream, 0)
    var addLoc = 0L
    var delLoc = 0L
    var modifierFlag = false
    gitService.visitDiffWithParent(commit, null, listOf(DiffEntry.ChangeType.MODIFY)) { diffEntry, _ ->
        if (diffEntry.newPath != newPath) return@visitDiffWithParent
        modifierFlag = true
        diffFormatter.format(diffEntry)
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


private fun writeCsv(result: ArrayList<List<Any>>): String {
    val sb = StringBuilder("path,modifierCount,addLocs,delLocs\n")
    result.forEach {
        sb.append(it.joinToString(",")).append("\n")
    }
    return sb.toString()
}