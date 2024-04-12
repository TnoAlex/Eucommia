package com.github.eucommia

import com.github.eucommia.handle.getAllHandle
import com.github.eucommia.util.ifFalse
import com.github.eucommia.util.ifTrue
import com.github.eucommia.util.removeComments
import com.github.gumtreediff.actions.Diff
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.GumtreeProperties
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileFilter
import java.io.FileOutputStream
import java.io.StringReader

private val handles = getAllHandle()
private var resultCollector = ArrayList<String>()
private val logger = LoggerFactory.getLogger("Eucommia")
private var resultStorePath = ""
private var count = 0

fun main(args: Array<String>) {
    val rootPath = args[0]
    val storeSuffixPath = args.getOrNull(1) ?: rootPath
    Run.initGenerators()
    val rootFile = File(rootPath)
    visitRootFiles(rootFile, storeSuffixPath)
}

private fun visitRootFiles(rootFile: File, storePath: String) {
    val isGitRepo = checkGitRepo(rootFile)
    isGitRepo.ifFalse {
        val gitRepos = rootFile.listFiles(FileFilter { it.isDirectory && checkGitRepo(it) })
        gitRepos?.forEachIndexed { index, repo ->
            logger.info("Start git repo: ${repo.name}, at ${index + 1} of ${gitRepos.size}")
            visitGitRepo(repo, storePath)
        }
    }
    isGitRepo.ifTrue {
        logger.info("Start git repo: ${rootFile.name}, at 1 total 1")
        visitGitRepo(rootFile, storePath)
    }
}

private fun visitGitRepo(gitRepo: File, storePath: String) {
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
    try {
        GitProcessor.visitGitRepo(gitRepo.path, ::findAstDiff)
    } catch (e: Exception) {
        logger.error("Error", e)
    }
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
    commitId: String, newFilePath: String, oldContent: String, newContent: String
) {
    try {
        val treeGenerator = getTreeGenerator(newFilePath.split(".").last())
        val newContentReader = StringReader(removeComments(newContent))
        val oldContentReader = StringReader(removeComments(oldContent))
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
            val tempFile = File("${resultStorePath}_$count$.json")
            if (!tempFile.exists()){
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