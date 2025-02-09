package com.github.tnoalex

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.gumtreediff.client.Run
import com.github.tnoalex.file.FileService
import com.github.tnoalex.git.GitManager
import com.github.tnoalex.utils.distillPath
import com.github.tnoalex.utils.excavateAstDiff
import com.github.tnoalex.utils.statsCommit
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger("Eucommia")

class CliParser: CliktCommand(name = "eucommia") {
    private val rootPath by argument(name = "rootPath", help = "The root path of git repositories").file(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true
    )

    private val storePath by argument(name = "storePath", help = "The store path of result").file(
        mustExist = false,
        canBeFile = false,
        canBeDir = true,
        mustBeWritable = true
    )

    private val mainRefname by option("-mr", help = "The branch ref of the git repository")

    private val visitModel by option("-vm", help = "Switch visitor model and stats model").int().default(0)

    private val commitDataPath by option(
        "-cdp",
        help = "The csv of commit to extract the git patch,[commit id,file path]"
    ).file(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true
    )

    private val maxDiffFileNumber by option(
        "-mdf",
        help = "Max number of changed files in a commit"
    ).int().default(Int.MAX_VALUE)

    override fun run() {
        (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(Logger.ROOT_LOGGER_NAME).level = Level.INFO
        Run.initGenerators()
        visitRootFiles()
    }

    private fun visitRootFiles() {
        GitManager.createGitServices(rootPath) { gitService ->
            val storeFullPath = storePath.canonicalPath
            val repoName = gitService.repoName
            when (visitModel) {
                0 -> {
                    val resultPath = Paths.get(storeFullPath, "$repoName.csv").pathString
                    val finishedPath =
                        Paths.get(storeFullPath, "$repoName-finished.csv").pathString
                    val finished = FileService.readLines(finishedPath)?.toSet() ?: emptySet()

                    excavateAstDiff(gitService, mainRefname, {
                        if (finished.contains(it.id.name)) {
                            return@excavateAstDiff false
                        }
                        FileService.writeAppend(finishedPath, it.id.name)
                        true
                    }, { it.size <= maxDiffFileNumber }) { astDiff ->
                        FileService.writeAppend(resultPath, astDiff.simpleToString())
                        logger.info { "${astDiff.commitId} done" }
                    }
                }

                1 -> {
                    statsCommit(gitService, mainRefname).also {
                        FileService.write(
                            Paths.get(storeFullPath, repoName, ".csv").pathString,
                            it.toByteArray()
                        )
                        logger.info { "$repoName done" }
                    }
                }

                2 -> {
                    commitDataPath?.let {
                        distillPath(
                            gitService,
                            Paths.get(it.canonicalPath, "$repoName.csv").toFile()
                        )
                    }?.let {
                        it.forEach { (k, v) ->
                            v.forEachIndexed { index, item ->
                                FileService.write(
                                    Paths.get(
                                        storeFullPath,
                                        repoName,
                                        k + "_$index.diff"
                                    ).pathString,
                                    item.toByteArray()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

fun main(args: Array<String>): Unit = CliParser().main(args)

