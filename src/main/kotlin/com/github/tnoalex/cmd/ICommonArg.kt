package com.github.tnoalex.cmd

import java.io.File

interface ICommonArg {
    val input: File
        get() = commonArg.input
    val output: File
        get() = commonArg.output
    val branchName: String?
        get() = commonArg.branchName
    val commonArg: CommonArg
}