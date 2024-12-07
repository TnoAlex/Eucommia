package com.github.tnoalex.handle

import com.github.gumtreediff.actions.TreeClassifier
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

abstract class AbstractHandler {
    companion object {
        private val subHandlers = mutableListOf<AbstractHandler>()

        init {
            Reflections(
                ConfigurationBuilder()
                    .forPackages("com.github.tnoalex")
                    .addScanners(Scanners.SubTypes)
            ).getSubTypesOf(AbstractHandler::class.java)
                .forEach {
                    val h =
                        it.declaredConstructors.first { d -> d.parameterCount == 0 }.newInstance() as AbstractHandler
                    subHandlers.add(h)
                }
        }

        fun handle(tree: TreeClassifier, collector: MutableList<String>) {
            subHandlers.forEach {
                if (it.attracted(tree)) {
                    collector.add(it.handleName)
                }
            }
        }
    }

    abstract val handleName: String

    abstract fun attracted(tree: TreeClassifier): Boolean
}