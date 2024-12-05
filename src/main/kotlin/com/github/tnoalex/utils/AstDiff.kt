package com.github.tnoalex.utils

data class AstDiff(
    val commitId: String,
    val filePaths: List<String>,
    val foundPattern: List<String>
) {
    companion object {
        @JvmStatic
        fun parse(str: String): AstDiff {
            val split = str.split(",")
            if (split.size != 3) {
                throw IllegalArgumentException("Invalid str for parse ${AstDiff::class.simpleName} $str")
            }
            return AstDiff(split[0], split[1].split(";"), split[2].split(";"))
        }
    }

    fun simpleToString(): String {
        return "$commitId,${filePaths.joinToString(";")},${foundPattern.joinToString(";")}"
    }
}