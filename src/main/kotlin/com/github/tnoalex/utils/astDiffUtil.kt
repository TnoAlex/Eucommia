package com.github.tnoalex.utils

import com.github.gumtreediff.actions.Diff
import com.github.gumtreediff.matchers.GumtreeProperties
import com.github.tnoalex.git.GitService
import com.github.tnoalex.handle.getAllHandle
import com.google.gson.Gson
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import java.io.InputStreamReader
import java.io.Reader

private val handles = getAllHandle()
private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))

fun excavateAstDiff(gitService: GitService, mainRef: String?): String {
    val collector = ArrayList<AstDiff>()
    gitService.visitCommit(mainRef, RevFilter.NO_MERGES) { revCommit ->
        gitService.visitDiffWithParent(
            revCommit,
            OR_PATH_FILTER,
            listOf(DiffEntry.ChangeType.MODIFY)
        ) { diff, reader ->
            val newLoader = reader.open(diff.newId.toObjectId())
            val oldLoader = reader.open(diff.oldId.toObjectId())
            val newContentReader = InputStreamReader(newLoader.openStream())
            val oldContentReader = InputStreamReader(oldLoader.openStream())
            val astDiff = findAstDiff(diff.newPath, oldContentReader, newContentReader)
            if (astDiff.isNotEmpty()) {
                collector.add(
                    AstDiff(
                        revCommit.id.name,
                        diff.newPath,
                        astDiff
                    )
                )
            }
        }
    }
    return writeResult(collector)
}

private fun findAstDiff(
    newFilePath: String,
    oldContentReader: Reader,
    newContentReader: Reader
): ArrayList<String> {
    val treeGenerator = getTreeGenerator(newFilePath.split(".").last())
    val diff = Diff.compute(oldContentReader, newContentReader, treeGenerator, null, GumtreeProperties())
    val allNodesClassifier = diff.createAllNodeClassifier()
    val collector = ArrayList<String>()
    handles.handle(allNodesClassifier, collector)
    return collector
}

private fun getTreeGenerator(fileExtensions: String): String {
    return when (fileExtensions) {
        "java" -> "java-treesitter-ng"
        "kt" -> "kotlin-treesitter-ng"
        else -> throw RuntimeException()
    }
}

private fun writeResult(collector: List<AstDiff>): String {
    return Gson().toJson(collector)
}