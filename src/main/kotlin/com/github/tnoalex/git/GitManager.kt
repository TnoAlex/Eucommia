package com.github.tnoalex.git

import com.github.tnoalex.utils.ifFalse
import com.github.tnoalex.utils.ifTrue
import java.io.File
import java.io.FileFilter

object GitManager {
    fun createGitServices(rootPath: File, consumer: (GitService) -> Unit) {
        val isGitRepo = checkGitRepo(rootPath)
        val gitServices = ArrayList<GitService>()
        isGitRepo.ifFalse {
            rootPath.listFiles(FileFilter { it.isDirectory && checkGitRepo(it) })
                ?.map { GitService(it) }
                ?.let { gitServices.addAll(it) }
        }
        isGitRepo.ifTrue {
            gitServices.add(GitService(rootPath))
        }
        gitServices.forEach {
            it.use(consumer)
        }
    }

    private fun checkGitRepo(file: File): Boolean {
        return file.listFiles(FileFilter { it.name == ".git" && it.isDirectory })?.isNotEmpty() ?: false
    }
}