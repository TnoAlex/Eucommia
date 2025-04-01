package com.github.tnoalex.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.tnoalex.file.FileService
import com.github.tnoalex.git.GitManager
import com.github.tnoalex.utils.statsCommitAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.pathString

class CommitStat : CliktCommand(name = "CommitStat"), ICommonArg {
    private val logger = KotlinLogging.logger {}
    override val commonArg: CommonArg by requireObject()

    override fun run() {
        GitManager.createGitServices(input) { gitService ->
            val storeFullPath = output.canonicalPath
            val repoName = gitService.repoName
            val file = File(Paths.get(storeFullPath, "${repoName}.csv").pathString)
            if (file.exists()) return@createGitServices
            statsCommitAsync(gitService, branchName).also {
                FileService.write(
                    Paths.get(storeFullPath, "${repoName}.csv").pathString,
                    it.toByteArray()
                )
                logger.info { "$repoName done" }
            }
        }
    }
}