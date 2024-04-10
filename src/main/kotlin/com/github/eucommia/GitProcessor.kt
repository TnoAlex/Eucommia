package com.github.eucommia

import java.io.File

object GitProcessor {
    private const val LIB_NAME = "eucommia_rust.dll"

    init {
        val libFile = File.createTempFile(LIB_NAME, null)
        val resourceAsStream = Thread.currentThread().contextClassLoader.getResourceAsStream(LIB_NAME)
        val allBytes = resourceAsStream!!.readAllBytes()
        libFile.writeBytes(allBytes)
        System.load(libFile.path)
    }

    @JvmStatic
    external fun visitGitRepo(
        path: String, visitor: (
            commitId: String, newFilePath: String, newContent: String, oldContent: String
        ) -> Unit
    )
}