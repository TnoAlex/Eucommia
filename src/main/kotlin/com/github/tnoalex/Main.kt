package com.github.tnoalex

import com.github.gumtreediff.actions.Diff
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.matchers.GumtreeProperties
import com.google.gson.Gson
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.filter.OrTreeFilter
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import java.io.*

private var git: Git? = null
private val handles = getAllHandle()
private var storeOutStream = FileOutputStream.nullOutputStream()

fun main(args: Array<String>) {
    val rootPath = args[0]
    val treeSitterPath = args[1]
    val storeSuffixPath = args[2]
    System.setProperty("gt.ts.path", treeSitterPath)
    Run.initGenerators()
    val rootFile = File(rootPath)
    visitRootFiles(rootFile, storeSuffixPath)
}

private fun visitRootFiles(rootFile: File, storePath: String) {
    rootFile.listFiles(FileFilter { it.isDirectory })?.forEach { gitRepoPath ->
        git = Git.open(gitRepoPath)
        val repo = git!!.repository
        val resultStorePath = "$storePath${File.pathSeparator}${gitRepoPath.name}.json"
        val resultStoreFile = File(resultStorePath)
        if (!resultStoreFile.exists()) {
            resultStoreFile.createNewFile()
        }
        storeOutStream = FileOutputStream(resultStoreFile)
        storeOutStream.use {
            repo.use { rep ->
                val mainRef = rep.findRef("refs/heads/main") ?: rep.findRef("refs/heads/master")
                RevWalk(rep).use { walk ->
                    visitCommits(walk, mainRef, rep)
                }
            }
        }
        storeOutStream = FileOutputStream.nullOutputStream()
    }
}

private fun visitCommits(revWalk: RevWalk, branch: Ref, repo: Repository) {
    val startCommit = revWalk.parseCommit(branch.objectId)
    revWalk.markStart(startCommit)
    revWalk.revFilter = RevFilter.NO_MERGES
    revWalk.forEach {
        visitDiffs(repo, it)
    }
}

private fun visitDiffs(repo: Repository, commit: RevCommit) {
    val newTree = CanonicalTreeParser()
    val oldTree = CanonicalTreeParser()
    val objectReader = repo.newObjectReader()
    objectReader.use { reader ->
        newTree.reset(reader, commit.tree.id)
        oldTree.reset(reader, commit.getParent(0).tree.id)

        val diffs = git!!.diff()
            .setNewTree(newTree)
            .setOldTree(oldTree)
            .setPathFilter(OR_PATH_FILTER)
            .call()
            .filter { it.changeType == DiffEntry.ChangeType.MODIFY }

        diffs.forEach { diff ->
            val newLoader = reader.open(diff.newId.toObjectId())
            val oldLoader = reader.open(diff.oldId.toObjectId())
            val newContentReader = InputStreamReader(newLoader.openStream())
            val oldContentReader = InputStreamReader(oldLoader.openStream())
            findAstDiff(
                newContentReader,
                oldContentReader,
                diff.newPath.split(".").last(),
                diff.newPath,
                commit.id.toString()
            )
        }
    }
}

private fun findAstDiff(
    newContentReader: Reader,
    oldContentReader: Reader,
    fileExtensions: String,
    filePath: String,
    commitId: String
) {
    val treeGenerator = getTreeGenerator(fileExtensions)
    val diff = Diff.compute(newContentReader, oldContentReader, treeGenerator, null, GumtreeProperties())
    val allNodesClassifier = diff.createAllNodeClassifier()
    val collector = ArrayList<String>()
    handles.handle(allNodesClassifier, collector)
    writeResult(commitId, filePath, collector)
}

private fun getTreeGenerator(fileExtensions: String): String {
    return when (fileExtensions) {
        "java" -> "java-javaparser"
        "kt" -> "kotlin-treesitter"
        else -> throw RuntimeException()
    }
}

private fun writeResult(commitId: String, filePath: String, collector: ArrayList<String>) {
    val res = mapOf("commitId" to commitId, "path" to filePath, "found" to collector)
    val json = Gson().toJson(res).toByteArray()
    storeOutStream.write(json)
}


private val OR_PATH_FILTER =
    OrTreeFilter.create(listOf(PathSuffixFilter.create(".kt"), PathSuffixFilter.create(".java")))