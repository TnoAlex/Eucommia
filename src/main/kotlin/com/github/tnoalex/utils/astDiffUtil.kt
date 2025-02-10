package com.github.tnoalex.utils

import com.github.gumtreediff.actions.Diff
import com.github.gumtreediff.matchers.GumtreeProperties
import com.github.tnoalex.git.GitService
import com.github.tnoalex.handle.AbstractHandler
import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import java.io.InputStreamReader
import java.io.Reader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

private val logger = KotlinLogging.logger {}
private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))

private fun shortenFilePaths(paths: List<String>): List<String> {
    val shortenedPathsMap = HashMap<String, ArrayList<String>>()
    for (path in paths) {
        val shorten = path.split("/").last()
        shortenedPathsMap.getOrPut(shorten) { ArrayList() }.add(path)
    }
    val result = ArrayList<String>()
    for ((shortenPath, originalPaths) in shortenedPathsMap) {
        if (originalPaths.size > 1) {
            result.addAll(originalPaths)
        } else {
            result.add(shortenPath)
        }
    }
    return result
}

fun excavateAstDiffAsync(
    gitService: GitService, mainRef: String?, commitFilter: suspend (RevCommit) -> Boolean,
    diffsFilter: (List<DiffEntry>) -> Boolean = { _ -> true },
    onFinish: () -> Unit,
    collector: suspend (AstDiff) -> Unit,
) {
    runBlocking {
        val commitChannel = Channel<RevCommit>(1024)
        logger.info { "start visit repo: ${gitService.repoName}" }
        launch { gitService.visitCommitAsync(mainRef, RevFilter.NO_MERGES, commitChannel) }
        for (revCommit in commitChannel) {
            launch(Dispatchers.IO) {
                revCommit.visitCommit(commitFilter, gitService, diffsFilter, collector)
            }
        }
    }
    onFinish()
    logger.info { "finish visit repo: ${gitService.repoName}" }
}

private suspend fun RevCommit.visitCommit(
    commitFilter: suspend (RevCommit) -> Boolean,
    gitService: GitService,
    diffsFilter: (List<DiffEntry>) -> Boolean,
    collector: suspend (AstDiff) -> Unit
) {
    val valuePlaceHolder = Any()
    val revCommit = this
    logger.info { "processing commit: ${revCommit.id.name}" }
    if (!commitFilter(revCommit)) {
        logger.info { "${revCommit.id.name} exists" }
        return
    }
    val filePaths = CopyOnWriteArrayList<String>()
    val astDiffs = ConcurrentHashMap<String, Any>()
    val tasks = mutableListOf<Deferred<*>>()
    gitService.visitDiffWithParentAsync(
        revCommit,
        OR_PATH_FILTER,
        listOf(DiffEntry.ChangeType.MODIFY),
        diffsFilter
    ) { diff, reader ->
        withContext(Dispatchers.IO) {
            val task = async {
                logger.debug { "start visit diff ${diff.newId} in commit ${revCommit.id.name}" }
                val newLoader = reader.open(diff.newId.toObjectId())
                val oldLoader = reader.open(diff.oldId.toObjectId())
                val newContentReader = InputStreamReader(newLoader.openStream())
                val oldContentReader = InputStreamReader(oldLoader.openStream())
                val astDiff = try {
                    findAstDiff(diff.newPath, oldContentReader, newContentReader)
                } catch (e: Exception) {
                    logger.error { e.stackTraceToString() }
                    emptyList()
                }
                if (astDiff.isNotEmpty()) {
                    filePaths.add(diff.newPath)
                    astDiffs.putAll(astDiff.map { it to valuePlaceHolder })
                }
            }
            tasks.add(task)
        }
    }
    tasks.awaitAll()
    logger.debug { "end collect commit ${revCommit.id.name}" }
    if (filePaths.isNotEmpty()) {
        collector(
            AstDiff(
                revCommit.id.name, shortenFilePaths(filePaths),
                astDiffs.toList().map { it.first }
            )
        )
    }
}


fun findAstDiff(
    newFilePath: String,
    oldContentReader: Reader,
    newContentReader: Reader
): ArrayList<String> {
    val treeGenerator = getTreeGenerator(newFilePath.split(".").last())
    val diff = Diff.compute(oldContentReader, newContentReader, treeGenerator, null, GumtreeProperties())
    val allNodesClassifier = diff.createAllNodeClassifier()
    val collector = ArrayList<String>()
    AbstractHandler.handle(allNodesClassifier, collector)
    return collector
}

fun getTreeGenerator(fileExtensions: String): String {
    return when (fileExtensions) {
        "java" -> "java-treesitter-ng"
        "kt" -> "kotlin-treesitter-ng"
        else -> throw RuntimeException()
    }
}

private fun writeResult(collector: List<AstDiff>): String {
    return Gson().toJson(collector)
}