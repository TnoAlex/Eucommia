package com.github.tnoalex

import com.github.ajalt.clikt.core.subcommands
import com.github.tnoalex.cmd.ASTDiff
import com.github.tnoalex.cmd.CliParser
import com.github.tnoalex.cmd.CommitData
import com.github.tnoalex.cmd.CommitStat

fun main(args: Array<String>): Unit =
    CliParser().subcommands(
        ASTDiff(),
        CommitStat(),
        CommitData()
    ).main(args)

