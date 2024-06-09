package com.github.tnoalex.utils

import com.github.tnoalex.file.FileService
import com.github.tnoalex.git.GitService
import org.eclipse.jgit.revwalk.RevCommit
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStreamReader


fun distillPath(gitService: GitService, commitDatePath: File, mainRef: String?): ArrayList<String>? {
    val commitData = getCommitData(commitDatePath) ?: return null
    val collector = ArrayList<String>()
    commitData.forEach {
        val commit = gitService.parseCommit(it) ?: return@forEach
        createGitPatch(commit, File(gitService.repoPath))?.let { res ->
            collector.add(res)
        }
    }
    return collector
}

private fun getCommitData(commitDatePath: File): List<String>? {
    try {
        val commitData = FileService.read(commitDatePath.canonicalPath)?.let { String(it) } ?: return null
        return commitData.split(System.lineSeparator()).filter { it.isNotBlank() }
    } catch (e: FileNotFoundException) {
        return null
    }
}

private fun createGitPatch(commit: RevCommit, workDir: File): String? {
    if (commit.parentCount <= 0) return null
    return invokeGit(commit.parents[0].id.name, commit.id.name(), workDir)
}

private fun invokeGit(formId: String, toId: String, workDir: File): String? {
    val processBuilder = ProcessBuilder("git", "format-patch", "$formId..$toId", "--stdout")
    processBuilder.directory(workDir)
    return runCatching {
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errReader = BufferedReader(InputStreamReader(process.errorStream))
        val sb = StringBuilder()
        reader.lines().forEach {
            sb.append(it).append("\n")
        }
        if (process.waitFor() != 0) {
            val esb = StringBuilder()
            errReader.lines().forEach {
                esb.append(it).append("\n")
            }
            logger.error("An error was encountered while processing  ${workDir.canonicalPath}\n err info: $esb")
        }
        sb.toString()
    }.getOrNull()
}

private val logger = LoggerFactory.getLogger("com.github.tnoalex.utils.patchDistill")