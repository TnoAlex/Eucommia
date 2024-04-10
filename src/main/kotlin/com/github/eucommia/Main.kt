package com.github.eucommia

import com.github.gumtreediff.actions.Diff
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.GumtreeProperties
import com.google.gson.Gson
import java.io.*

private val handles = getAllHandle()
private var storeOutStream = FileOutputStream.nullOutputStream()

fun main(args: Array<String>) {
    val rootPath = args[0]
    val storeSuffixPath = args.getOrNull(1) ?: rootPath
    Run.initGenerators()
    val rootFile = File(rootPath)
    visitRootFiles(rootFile, storeSuffixPath)
}

private fun visitRootFiles(rootFile: File, storePath: String) {
    rootFile.listFiles(FileFilter { it.isDirectory })?.forEach { gitRepoPath ->
        val resultStorePath = "$storePath${File.pathSeparator}${gitRepoPath.name}.json"
        val resultStoreFile = File(resultStorePath)
        if (!resultStoreFile.exists()) {
            resultStoreFile.createNewFile()
        }
        storeOutStream = FileOutputStream(resultStoreFile)
        storeOutStream.use {
            GitProcessor.visitGitRepo(gitRepoPath.path, ::findAstDiff)
        }
        storeOutStream = FileOutputStream.nullOutputStream()
    }
}


private fun findAstDiff(
    commitId: String, newFilePath: String, newContent: String, oldContent: String
) {
    val treeGenerator = getTreeGenerator(newFilePath.split(".").last())
//    val newContentReader = StringBuilder(newContent).toString().reader()
//    val oldContentReader = StringBuilder(oldContent).toString().reader()
//    val newContentReader = newContent.reader()
//    val oldContentReader = oldContent.reader()
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