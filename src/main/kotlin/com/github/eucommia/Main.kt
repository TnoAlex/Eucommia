package com.github.eucommia

import com.github.eucommia.handle.getAllHandle
import com.github.eucommia.util.ifFalse
import com.github.eucommia.util.ifTrue
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
private var storeOutStream = FileOutputStream.nullOutputStream()
private val logger = LoggerFactory.getLogger("Eucommia")

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
    val resultStorePath = "$storePath${File.separatorChar}${gitRepo.name}.json"
    val resultStoreFile = File(resultStorePath)
    if (!resultStoreFile.exists()) {
        resultStoreFile.createNewFile()
    }
    storeOutStream = FileOutputStream(resultStoreFile)
    storeOutStream.use {
        GitProcessor.visitGitRepo(gitRepo.path, ::findAstDiff)
    }
    storeOutStream = FileOutputStream.nullOutputStream()
    logger.info("Finished git repo: ${gitRepo.name},result has be wrote in: $resultStorePath")
}

private fun checkGitRepo(file: File): Boolean {
    return file.listFiles(FileFilter { it.name == ".git" && it.isDirectory })?.isNotEmpty() ?: false
}

private fun findAstDiff(
    commitId: String, newFilePath: String, oldContent: String, newContent: String
) {
    val treeGenerator = getTreeGenerator(newFilePath.split(".").last())
    val newContentReader = StringReader(newContent)
    val oldContentReader = StringReader(oldContent)
    val diff = Diff.compute(oldContentReader, newContentReader, treeGenerator, null, GumtreeProperties())
    val allNodesClassifier = diff.createAllNodeClassifier()
    val collector = ArrayList<String>()
    handles.handle(allNodesClassifier, collector)
    writeResult(commitId, newFilePath, collector)
}

private fun getTreeGenerator(fileExtensions: String): String {
    return when (fileExtensions) {
        "java" -> "java-treesitter-ng"
        "kt" -> "kotlin-treesitter-ng"
        else -> throw RuntimeException()
    }
}

private fun writeResult(commitId: String, filePath: String, collector: ArrayList<String>) {
    val res = mapOf("commitId" to commitId, "path" to filePath, "found" to collector)
    val json = Gson().toJson(res).toByteArray()
    storeOutStream.write(json)
}