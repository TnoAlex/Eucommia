package com.github.tnoalex.handle

import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.*
import kotlin.system.exitProcess


object HandlerTestGenerator {
    private val logger = KotlinLogging.logger {}
    private val gson = Gson()
    private const val TEST_DATA_PATH = "testData"
    private const val TEST_OUTPUT_PATH =
        "src/test/kotlin/com/github/tnoalex/handle/generated/HandlerTest.kt"
    private const val IGNORE_FILE_NAME = "ignore"
    private const val CONFIG_FILE_NAME = "config.json"
    private const val MAIN_TEST_CLASS_NAME = "HandlerTest"
    private val sb = StringBuilder()
    private val fileStateStack = Stack<Pair<File, StringBuilder>>()

    /**
     * Invoke when enter a test root directory.
     *
     * A test root always contains a text file named "result".
     *
     * @param dir test root directory
     */
    private fun enterTestRootDir(dir: File) {
        val testNameDir = dir.path.split(File.separator).last()
        val testName = "test_$testNameDir"
        val (file, sb) = fileStateStack.peek()
        require(File(file, testNameDir) == dir)
        val ignoreFile = File(dir, IGNORE_FILE_NAME)
        val configReader = File(dir, CONFIG_FILE_NAME).reader()
        val config = gson.fromJson(configReader, HandlerTestConfig::class.java)
        sb.append(
            """
            |@Test${if (ignoreFile.exists()) "\n@Disabled" else ""}
            |fun $testName() {
            |    doValidate(
            |        ${"\"\"\""}${dir.path}${"\"\"\""},
            |        ${"\"\"\""}${config.newName}${"\"\"\""},
            |        ${"\"\"\""}${config.oldName}${"\"\"\""},
            |        listOf<String>(${config.result.joinToString(", ") { "\"$it\"" }})
            |    )
            |}
            |
        """.replaceIndentByMargin(" " * 4)
        )
        sb.append(System.lineSeparator())
    }

    /**
     * invoke when enter sub-test-directory in order to generate test classes.
     *
     * For example, test directory in [TEST_DATA_PATH] is "import/myImportTest1/",
     * the class "Import" inside [MAIN_TEST_CLASS_NAME] while be generated.
     */
    private fun enterSubTestDir(dir: File) {
        fileStateStack.push(dir to StringBuilder())
    }

    private fun CharSequence.prependIndent(indent: String = "    "): String =
        lineSequence()
            .map {
                when {
                    it.isBlank() -> {
                        when {
                            it.length < indent.length -> indent
                            else -> it
                        }
                    }

                    else -> indent + it
                }
            }
            .joinToString("\n")

    private fun leaveSubTestDir(dir: File) {
        val (_, sb) = fileStateStack.pop()
        if (sb.isEmpty()) return
        val (_, parentSb) = fileStateStack.peek()
        val className = dir.path.split(File.separator).last()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val sbNow = StringBuilder()
        sbNow.append(System.lineSeparator())
        sbNow.append("@Nested")
        sbNow.append(System.lineSeparator())
        sbNow.append("""inner class ${className}Test {""")
        sbNow.append(System.lineSeparator())
        sbNow.append(sb)
        sbNow.append(System.lineSeparator())
        sbNow.append("}")
        sbNow.append(System.lineSeparator())
        parentSb.append(sbNow.prependIndent())
    }

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Start HandlerTestGenerator" }
        if (sb.isNotEmpty()) {
            logger.error { "Test Class Has Already Generated!" }
            exitProcess(-1)
        }
        sb.append(
            """
            |// auto generated, do not manually edit!
            |package com.github.tnoalex.handle.generated
            |
            |import com.github.tnoalex.handle.BaseHandlerTest
            |import org.junit.jupiter.api.Nested
            |import org.junit.jupiter.api.Test
            |import org.junit.jupiter.api.Disabled
            |import org.junit.jupiter.api.BeforeAll
            |import com.github.gumtreediff.client.Run
            |
            |class $MAIN_TEST_CLASS_NAME : BaseHandlerTest() {
            |   
            |    companion object {
            |        @JvmStatic
            |        @BeforeAll
            |        fun init() {
            |            Run.initGenerators()
            |        }
            |    }
            |
        """.trimMargin()
        )
        val testPath = File(TEST_DATA_PATH)
        fileStateStack.push(testPath to sb)
        val fileTreeWalk = testPath.walkTopDown()
            .onEnter {
                if (it == testPath) return@onEnter true
                if (it.isDirectory && File(it, CONFIG_FILE_NAME).exists()) {
                    enterTestRootDir(it)
                    return@onEnter true
                }
                enterSubTestDir(it)
                return@onEnter true
            }.onLeave {
                if (it == testPath) return@onLeave
                if (it.isDirectory && File(it, CONFIG_FILE_NAME).exists()) {
                    return@onLeave
                }
                leaveSubTestDir(it)
                return@onLeave
            }
        fileTreeWalk.forEach { _ -> }
        sb.append(System.lineSeparator())
        sb.append("}")
        val testOut = File(TEST_OUTPUT_PATH)
        if (testOut.exists()) {
            testOut.deleteRecursively()
        }
        if (!testOut.parentFile.exists()) {
            testOut.parentFile.mkdirs()
        }
        testOut.writeText(sb.toString())
    }
}

private operator fun String.times(i: Int): String {
    val sb = StringBuilder()
    for (i1 in 0 until i) {
        sb.append(this)
    }
    return sb.toString()
}