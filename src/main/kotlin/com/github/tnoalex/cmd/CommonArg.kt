package com.github.tnoalex.cmd

import java.io.File

data class CommonArg(
    val input: File,
    val output: File,
    val branchName: String?
)