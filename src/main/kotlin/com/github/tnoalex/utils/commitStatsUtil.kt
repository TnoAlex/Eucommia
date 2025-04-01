package com.github.tnoalex.utils

import com.github.tnoalex.git.GitService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))

private val logger = KotlinLogging.logger {}

fun statsCommitAsync(gitService: GitService, mainRef: String?): String {
    // key: pathString
    // value: <modifyCount, addLineCount, deleteLineCount>
    val repoStats = ConcurrentHashMap<String, Triple<Long, Long, Long>>()
    val renameStats = ConcurrentHashMap<String, String>()
    val fileTreeWalk = File(gitService.repoPath).walkTopDown().onEnter {
        if (it.endsWith(".git") || it.endsWith(".idea") || it.endsWith("build")) {
            return@onEnter false
        }
        true
    }
    for (file in fileTreeWalk) {
        val filePath = file.path
        if (filePath.endsWith(".kt") || filePath.endsWith(".java")) {
            val simplePath = filePath.removePrefix(gitService.repoPath).removePrefix("/")
            repoStats.compute(simplePath) { _, _ ->
                logger.trace { "find file: $simplePath" }
                Triple(0, 0, 0)
            }
        }
    }
    runBlocking(Dispatchers.IO) {
        val commitChannel = Channel<RevCommit>(2048)
        logger.info { "start visit repo: ${gitService.repoName}" }
        launch { gitService.visitCommitAsync(mainRef, RevFilter.NO_MERGES, commitChannel) }
        for (commit in commitChannel) {
            logger.trace { "visit commit: ${commit.id.name}" }
            diffVisit(commit, gitService, renameStats, repoStats)
            logger.trace { "end visit commit: ${commit.id.name}" }
        }
        logger.info { "finish visit repo: ${gitService.repoName}" }
    }

    logger.trace { "ready to write csv with size of repoStats is: {${repoStats.size}}" }
    return writeCsv(repoStats.map { listOf(it.key, it.value.first, it.value.second, it.value.third) })
}

private val formatDiffRegex = Regex("@@.*?@@")

private fun diffVisit(
    commit: RevCommit,
    gitService: GitService,
    renameStats: ConcurrentHashMap<String, String>,
    intermediateResults: ConcurrentHashMap<String, Triple<Long, Long, Long>>
) {
    tailrec fun currentName(old: String): String {
        if (renameStats.containsKey(old)) {
            val mayBeResult = renameStats[old]!!
            if (mayBeResult == old) {
                return mayBeResult
            }
            return currentName(mayBeResult)
        }
        return old
    }

    if (commit.parentCount <= 0) return
    val outputStream = ByteArrayOutputStream()
    gitService.visitDiffWithParent(
        commit,
        OR_PATH_FILTER,
        listOf(DiffEntry.ChangeType.MODIFY)
    ) { diffEntry, _ ->
        val oldPath = diffEntry.oldPath
        val newPath = diffEntry.newPath
        if (newPath != oldPath && oldPath != "/dev/null" && newPath != "/dev/null") {
            synchronized(renameStats) {
                renameStats[oldPath] = currentName(newPath)
                logger.trace { "link old name $oldPath to new name $newPath" }
            }
        }

        val diffFormatter = gitService.getDiffFormatter(outputStream, 0)
        val currentName = currentName(newPath)
        if (!intermediateResults.containsKey(currentName)) return@visitDiffWithParent
        if (newPath != currentName) {
            logger.trace { "${diffEntry.newPath} ----> $currentName" }
        }
        diffFormatter.format(diffEntry)
        val formattedDiff = outputStream.toString().split(formatDiffRegex).last()
        intermediateResults.compute(currentName) { _, oldV ->
            var (modifyCount, addLoc, delLoc) = oldV!! // we have already check the key exists
            modifierStats(formattedDiff).also { s ->
                modifyCount++
                addLoc += s.first
                delLoc += s.second
            }
            return@compute Triple(modifyCount, addLoc, delLoc)
        }
        outputStream.reset()
    }
}

private fun modifierStats(text: String): Pair<Long, Long> {
    var addLoc = 0L
    var delLoc = 0L
    text.lines().forEach {
        if (it.isBlank()) return@forEach
        if (it.trimStart().startsWith("+")) addLoc++
        else if (it.trimStart().startsWith("-")) delLoc++
    }
    return Pair(addLoc, delLoc)
}


private fun writeCsv(result: List<List<Any>>): String {
    val sb = StringBuilder("path,modifierCount,addLocs,delLocs\n")
    result.forEach {
        sb.append(it.joinToString(",")).append("\n")
    }
    return sb.toString()
}