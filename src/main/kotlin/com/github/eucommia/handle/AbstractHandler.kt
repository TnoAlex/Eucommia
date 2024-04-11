package com.github.eucommia.handle

import com.github.gumtreediff.actions.TreeClassifier

abstract class AbstractHandler {
    private var nextHandle: AbstractHandler? = null
    abstract val handleName: String

    fun addHandle(abstractHandler: AbstractHandler) {
        if (nextHandle == null) {
            nextHandle = abstractHandler
        } else {
            nextHandle!!.addHandle(abstractHandler)
        }
    }

    fun handle(tree: TreeClassifier, collector: MutableList<String>) {
        val attractedIndex = attracted(tree)
        if (attractedIndex) {
            collector.add(handleName)
        }
        nextHandle?.handle(tree, collector)
    }

    abstract fun attracted(tree: TreeClassifier): Boolean

    class DefaultHandler : AbstractHandler() {
        override val handleName: String
            get() = "default"

        override fun attracted(tree: TreeClassifier): Boolean {
            return false
        }
    }
}