package com.github.tnoalex.handle

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

fun getAllHandle(): AbstractHandler {
    val handle = AbstractHandler.DefaultHandler()
    Reflections(
        ConfigurationBuilder()
            .forPackages("com.github.tnoalex")
            .addScanners(Scanners.SubTypes)
    ).getSubTypesOf(AbstractHandler::class.java)
        .filter { it.javaClass != AbstractHandler.DefaultHandler::class.java }
        .forEach {
            val h = it.declaredConstructors.first { d -> d.parameterCount == 0 }.newInstance() as AbstractHandler
            handle.addHandle(h)
        }
    return handle
}