package com.github.tnoalex

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

fun getAllHandle(): AbstractHandle {
    val handle = AbstractHandle.DefaultHandle()
    Reflections(
        ConfigurationBuilder()
            .forPackages("com.github.tnoalex")
            .addScanners(Scanners.SubTypes)
    ).getSubTypesOf(AbstractHandle::class.java)
        .filter { it.javaClass != AbstractHandle.DefaultHandle::class.java }
        .forEach {
            val h = it.declaredConstructors.first { d -> d.parameterCount == 0 }.newInstance() as AbstractHandle
            handle.addHandle(h)
        }
    return handle
}