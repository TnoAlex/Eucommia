package com.github.tnoalex

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.gumtreediff.actions.Diff
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.GumtreeProperties
import com.github.tnoalex.handle.getAllHandle
import com.github.tnoalex.util.ifFalse
import com.github.tnoalex.util.ifTrue
import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.Reader

private val handles = getAllHandle()
private var resultCollector = ArrayList<String>()
private val logger = LoggerFactory.getLogger("Eucommia")
private var resultStorePath = ""
private var count = 0


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

    private val visitModel by option("-vm", help = "Switch visitor model and stats model").int().default(1)

    override fun run() {
        (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(Logger.ROOT_LOGGER_NAME).level = Level.INFO
        Run.initGenerators()
        visitRootFiles(rootPath, storePath.canonicalPath, visitModel, mainRefname)
    }

}

fun main(args: Array<String>) = CliParser().main(args)

private fun  visitRootFiles(rootFile: File, storePath: String, visitModel: Int, mainRefName: String?) {
    val isGitRepo = checkGitRepo(rootFile)
    isGitRepo.ifFalse {
        val gitRepos = rootFile.listFiles(FileFilter { it.isDirectory && checkGitRepo(it) })
        gitRepos?.forEachIndexed { index, repo ->
            logger.info("Start git repo: ${repo.name}, at ${index + 1} of ${gitRepos.size}")
            if (visitModel == 1) {
                visitGitRepo(repo, storePath, mainRefName)
            } else {
                stats(repo, File(storePath))
            }
        }
    }
    isGitRepo.ifTrue {
        logger.info("Start git repo: ${rootFile.name}, at 1 total 1")
        if (visitModel == 1) {
            visitGitRepo(rootFile, storePath, mainRefName)
        } else {
            stats(rootFile, File(storePath))
        }
    }
}

private fun visitGitRepo(gitRepo: File, storePath: String, mainRefName: String?) {
    resultStorePath = "$storePath${File.separatorChar}${gitRepo.name}"
    var resultStoreFile = File(resultStorePath)
    if (!resultStoreFile.exists()) {
        resultStoreFile.mkdirs()
    }
    resultStorePath = "$resultStorePath${File.separatorChar}${gitRepo.name}"
    resultStoreFile = File("$resultStorePath.json")
    if (!resultStoreFile.exists()) {
        resultStoreFile.createNewFile()
    }
    val storeOutStream = FileOutputStream(resultStoreFile)
    val visitor = GitCommitsVisitor(gitRepo, mainRefName, ::findAstDiff)
    visitor.visitGitRepo()
    storeOutStream.use {
        it.write(Gson().toJson(resultCollector).toByteArray())
        resultCollector.clear()
        count = 0
    }
    logger.info("Finished git repo: ${gitRepo.name},result has be wrote in: $resultStorePath")
}

private fun checkGitRepo(file: File): Boolean {
    return file.listFiles(FileFilter { it.name == ".git" && it.isDirectory })?.isNotEmpty() ?: false
}

private fun findAstDiff(
    commitId: String, newFilePath: String, oldContentReader: Reader, newContentReader: Reader
) {
    try {
        val treeGenerator = getTreeGenerator(newFilePath.split(".").last())
        val diff = Diff.compute(oldContentReader, newContentReader, treeGenerator, null, GumtreeProperties())
        val allNodesClassifier = diff.createAllNodeClassifier()
        val collector = ArrayList<String>()
        handles.handle(allNodesClassifier, collector)
        writeResult(commitId, newFilePath, collector)
    } catch (e: Exception) {
        logger.error("visit git repo failed", e)
    }
}

private fun getTreeGenerator(fileExtensions: String): String {
    return when (fileExtensions) {
        "java" -> "java-treesitter-ng"
        "kt" -> "kotlin-treesitter-ng"
        else -> throw RuntimeException()
    }
}

private fun writeResult(commitId: String, filePath: String, collector: ArrayList<String>) {
    collector.isNotEmpty().ifTrue {
        val res = mapOf("commitId" to commitId, "path" to filePath, "found" to collector)
        val json = Gson().toJson(res)
        resultCollector.add(json)
        count++
        if (count % 5 == 0) {
            val tempFile = File("${resultStorePath}_$count.json")
            if (!tempFile.exists()) {
                tempFile.createNewFile()
            }
            val outputStream = FileOutputStream(tempFile)
            outputStream.use { s ->
                val tmpJson = Gson().toJson(resultCollector.subList(count - 5, count))
                s.write(tmpJson.toByteArray())
            }
        }
    }
}