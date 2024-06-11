package com.github.tnoalex.utils

import com.github.tnoalex.file.FileService
import com.github.tnoalex.git.GitService
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException


fun distillPath(gitService: GitService, commitDatePath: File): HashMap<String, String>? {
    val commitData = getCommitData(commitDatePath) ?: return null
    val collector = HashMap<String, String>()
    commitData.forEach { (k, v) ->
        val commit = gitService.parseCommit(k) ?: return@forEach
        createGitPatch(commit, gitService, v)?.let { res ->
            collector[k.substring(0..7)] = res
        }
    }
    return collector
}

private fun getCommitData(commitDatePath: File): Map<String, String>? {
    try {
        val commitData = FileService.read(commitDatePath.canonicalPath)?.let { String(it) } ?: return null
        return commitData.split(System.lineSeparator()).filter { it.isNotBlank() }.map { it.split(",") }
            .associate { it[0] to it[1] }
    } catch (e: FileNotFoundException) {
        return null
    }
}

private fun createGitPatch(commit: RevCommit, gitService: GitService, filePath: String): String? {
    if (commit.parentCount <= 0) return null
    val outputStream = ByteArrayOutputStream()
    val diffFormatter = gitService.getDiffFormatter(outputStream, 5)
    val res = ArrayList<String>()
    val oldCmit = gitService.parseCommit(commit.parents[0].id.name()) ?: return null
    gitService.visitDiff(
        oldCmit,
        commit,
        PathSuffixFilter.create(filePath),
        listOf(DiffEntry.ChangeType.MODIFY)
    ) { diffEntry, _ ->
        diffFormatter.format(diffEntry)
        res.add(outputStream.toString())
        outputStream.reset()
    }
    return if (res.isNotEmpty()) res[0] else null
}


private val logger = LoggerFactory.getLogger("com.github.tnoalex.utils.patchDistill")