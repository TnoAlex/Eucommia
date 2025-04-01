package com.github.tnoalex.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.tnoalex.file.FileService
import com.github.tnoalex.git.GitManager
import com.github.tnoalex.utils.distillPath
import java.nio.file.Paths
import kotlin.io.path.pathString

class CommitData : CliktCommand(name = "CommitData"), ICommonArg {

    override val commonArg: CommonArg by requireObject()
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
        GitManager.createGitServices(input) { gitService ->
            val storeFullPath = output.canonicalPath
            val repoName = gitService.repoName
            commitDataPath?.let {
                distillPath(
                    gitService,
                    Paths.get(it.canonicalPath, "$repoName.csv").toFile()
                )
            }?.let {
                it.forEach { (k, v) ->
                    v.forEachIndexed { index, item ->
                        FileService.write(
                            Paths.get(
                                storeFullPath,
                                repoName,
                                k + "_$index.diff"
                            ).pathString,
                            item.toByteArray()
                        )
                    }
                }
            }
        }
    }
}