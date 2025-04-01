package com.github.tnoalex.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file

class CliParser : CliktCommand(name = "eucommia") {
    private val input by argument(name = "input", help = "The input path of git repositories").file(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true
    )

    private val output by argument(name = "output", help = "The output path of result").file(
        mustExist = false,
        canBeFile = false,
        canBeDir = true,
        mustBeWritable = true
    )

    private val branchName by option("-b", help = "The branch name of the git repository")

    override fun run() {
        currentContext.findOrSetObject {
            CommonArg(input, output, branchName)
        }
        val subcommand = currentContext.invokedSubcommand
        if (subcommand == null) {
            echo("invoked without a subcommand")
        } else {
            echo("about to run ${subcommand.commandName}")
        }
    }

}