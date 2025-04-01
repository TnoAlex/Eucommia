package com.github.tnoalex.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.gumtreediff.client.Run
import com.github.tnoalex.file.FileService
import com.github.tnoalex.git.GitManager
import com.github.tnoalex.utils.distillPath
import com.github.tnoalex.utils.excavateAstDiffAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString

class ASTDiff : CliktCommand(name = "ASTDiff"), ICommonArg {
    private val logger = KotlinLogging.logger {}
    override val commonArg: CommonArg by requireObject()

    private val maxDiffFileNumber by option(
        "-mdf",
        help = "Max number of changed files in a commit"
    ).int().default(Int.MAX_VALUE)

    override fun run() {
        Run.initGenerators()
        visitRootFiles()
    }

    private fun visitRootFiles() {
        GitManager.createGitServices(input) { gitService ->
            val storeFullPath = output.canonicalPath
            val repoName = gitService.repoName
            val resultPath = Paths.get(storeFullPath, "$repoName.csv").pathString
            val finishedPath = Paths.get(storeFullPath, "$repoName-finished.csv").pathString
            val finished = FileService.readLines(finishedPath)?.map {
                val r = it.substring(0, 40)
                require(r.length == 40)
                r
            }?.toSet() ?: emptySet()
            val finishedCommitChannel = Channel<String>(65536)
            val resultChannel = Channel<String>(65536)
            excavateAstDiffAsync(gitService, branchName,
                commitFilter = {
                    if (finished.contains(it.id.name)) {
                        return@excavateAstDiffAsync false
                    }
                    finishedCommitChannel.send(it.id.name)
                    true
                }, diffsFilter = { it.size <= maxDiffFileNumber },
                onFinish = {
                    finishedCommitChannel.close()
                    resultChannel.close()
                }) { astDiff -> resultChannel.send(astDiff.simpleToString()) }

            runBlocking {
                val writeFinished = async {
                    for (finishedId in finishedCommitChannel) {
                        FileService.writeAppend(finishedPath, finishedId)
                        logger.info { "finished $finishedId" }
                    }
                }
                val writeResult = async {
                    for (resultId in resultChannel) {
                        FileService.writeAppend(resultPath, resultId)
                        logger.info { "write result: $resultId" }
                    }
                }
                awaitAll(writeFinished, writeResult)
            }
        }
    }
}