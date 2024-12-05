package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class ExceptionMarkHandler : AbstractHandler() {
    override val handleName: String
        get() = "exception_mark"

    override fun attracted(tree: TreeClassifier): Boolean {
        val inserted = tree.insertedDsts
        val node = inserted.firstOrNull {
            it.type.name == "class_modifier" &&
                    it.children.any { c -> c.label == "@" }&&
                    it.children.any { c -> c.label == "Throws" }
        }
        return node != null && node.parent !in inserted
    }
}