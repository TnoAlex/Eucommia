package com.github.tnoalex.utils

data class AstDiff(
    val commitId: String,
    val filePath: String,
    val foundPattern: List<String>
)