package com.github.tnoalex.file

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

object FileService {
    fun write(filePath: String, content: ByteArray) {
        val targetFile = File(filePath)
        val parentFile = targetFile.parentFile
        if (!parentFile.exists()) {
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

    fun read(filePath: String): ByteArray? {
        val file = File(filePath).also { if (!it.exists()) throw FileNotFoundException(filePath) }
        val inputStream = FileInputStream(file)
        inputStream.use {
            return it.readAllBytes()
        }
    }
}