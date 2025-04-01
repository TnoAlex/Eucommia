package com.github.tnoalex.file

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

object FileService {
    fun write(filePath: String, content: ByteArray) {
        val targetFile = File(filePath)
        val parentFile = targetFile.parentFile
        if (parentFile != null && !parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (!targetFile.exists()) {
            targetFile.createNewFile()
        }
        val outputStream = FileOutputStream(targetFile)
        outputStream.use {
            it.write(content)
        }
    }

    fun writeAppend(filePath: String, content: String) {
        val targetFile = File(filePath)
        val parentFile = targetFile.parentFile
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (!targetFile.exists()) {
            targetFile.createNewFile()
        }
        targetFile.appendText(content + "\n")
    }

    fun readAllBytes(filePath: String): ByteArray? {
        val file = File(filePath).also { if (!it.exists()) throw FileNotFoundException(filePath) }
        val inputStream = FileInputStream(file)
        inputStream.use {
            return it.readAllBytes()
        }
    }

    fun readLines(filePath: String): List<String>? {
        val file = File(filePath).also { if (!it.exists()) return null }
        val inputStream = FileInputStream(file)
        inputStream.use {
            return it.reader().readLines()
        }
    }
}