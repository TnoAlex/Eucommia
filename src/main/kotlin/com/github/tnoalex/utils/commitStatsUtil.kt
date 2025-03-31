package com.github.tnoalex.utils

import com.github.tnoalex.git.GitService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))

private val logger = KotlinLogging.logger {}

fun statsCommitAsync(gitService: GitService, mainRef: String?): String {
    // key: pathString
    // value: <modifyCount, addLineCount, deleteLineCount>
    val repoStats = ConcurrentHashMap<String, Triple<Long, Long, Long>>()

    runBlocking {
        val jobs = mutableListOf<Job>()
        gitService.visitTreeAsync(mainRef, OR_PATH_FILTER, true) { treeWalk ->
            val path = treeWalk.pathString
            gitService.visitLogsAsync({ logCommand ->
                logCommand.addPath(treeWalk.pathString)
                    .setRevFilter(RevFilter.NO_MERGES)
                    .all()
            }) { commit ->
                logger.trace { "visit commit: ${commit.id.name}" }
                val job = CoroutineScope(Dispatchers.IO).launch inner@{
                    val (flag, add, del) = run {
                        val r = diffVisit(commit, path, gitService)
                        logger.trace { "finish diff visit commit: ${commit.id.name}" }
                        r
                    } ?: return@inner
                    repoStats.compute(path) { _, v ->
                        var (oldModifyCount, oldAddLineCount, oldDeleteLineCount) = v
                            ?: Triple(0L, 0L, 0L)
                        if (flag) {
                            oldModifyCount++
                        }
                        oldAddLineCount += add
                        oldDeleteLineCount += del
                        logger.trace { "recorded commit: ${commit.id.name}" }
                        return@compute Triple(oldModifyCount, oldAddLineCount, oldDeleteLineCount)
                    }
                }
                jobs.add(job)
            }
        }
        jobs.joinAll()
    }

    logger.trace { "ready to write csv with size of repoStats is: {${repoStats.size}}" }
    return writeCsv(repoStats.map { listOf(it.key, it.value.first, it.value.second, it.value.third) })
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


private fun writeCsv(result: List<List<Any>>): String {
    val sb = StringBuilder("path,modifierCount,addLocs,delLocs\n")
    result.forEach {
        sb.append(it.joinToString(",")).append("\n")
    }
    return sb.toString()
}