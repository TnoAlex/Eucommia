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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString

private val logger = LoggerFactory.getLogger("Eucommia")


class CliParser : CliktCommand(name = "eucommia") {
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

    override fun run() {
        (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(Logger.ROOT_LOGGER_NAME).level = Level.INFO
        Run.initGenerators()
        visitRootFiles(rootPath, storePath.canonicalPath, visitModel, mainRefname, commitDataPath)
    }

}

fun main(args: Array<String>): Unit = CliParser().main(args)

private fun visitRootFiles(
    rootFile: File,
    storePath: String,
    visitModel: Int,
    mainRefName: String?,
    commitDataPath: File?
) {
    GitManager.createGitServices(rootFile) { gitService ->
        when (visitModel) {
            0 -> {
                excavateAstDiff(gitService, mainRefName).also {
                    FileService.write(Paths.get(storePath, gitService.repoName, ".json").pathString, it.toByteArray())
                    logger.info("${gitService.repoName} done")
                }
            }

            1 -> {
                statsCommit(gitService, mainRefName).also {
                    FileService.write(Paths.get(storePath, gitService.repoName, ".csv").pathString, it.toByteArray())
                    logger.info("${gitService.repoName} done")
                }
            }

            2 -> {
                commitDataPath?.let {
                    distillPath(
                        gitService,
                        Paths.get(it.canonicalPath, "${gitService.repoName}.csv").toFile()
                    )
                }?.let {
                    it.forEach { (k, v) ->
                        v.forEachIndexed { index, item ->
                            FileService.write(
                                Paths.get(storePath, gitService.repoName, k + "_$index.diff").pathString,
                                item.toByteArray()
                            )
                        }
                    }
                }
            }
        }
    }
}
