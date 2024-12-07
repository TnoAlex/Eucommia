package com.github.tnoalex.handle

import com.github.tnoalex.utils.findAstDiff
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.io.FileNotFoundException

abstract class BaseHandlerTest {

    private fun File.assertExists(): File {
        if (!exists()) {
            throw FileNotFoundException(path)
        }
        return this
    }

    fun doValidate(path: String, newName: String, oldName: String, result: List<String>) {
        val testDir = File(path).assertExists()
        val newFile = File(testDir, newName).assertExists()
        val oldFile = File(testDir, oldName).assertExists()
        val astDiff = findAstDiff(newName, oldFile.reader(), newFile.reader())
        assertEquals(result.sorted(), astDiff.sorted())
    }
}