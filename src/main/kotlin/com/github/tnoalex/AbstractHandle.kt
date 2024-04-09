package com.github.tnoalex

import com.github.gumtreediff.actions.TreeClassifier

abstract class AbstractHandle {
    private var nextHandle: AbstractHandle? = null
    abstract val handleName: String

    fun addHandle(abstractHandle: AbstractHandle) {
        if (nextHandle == null) {
            nextHandle = abstractHandle
        } else {
            nextHandle!!.addHandle(abstractHandle)
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

    class DefaultHandle : AbstractHandle() {
        override val handleName: String
            get() = "default"

        override fun attracted(tree: TreeClassifier): Boolean {
            return false
        }
    }
}