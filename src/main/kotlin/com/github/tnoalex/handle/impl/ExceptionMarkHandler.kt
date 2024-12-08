package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class ExceptionMarkHandler: AbstractHandler() {
    override val handleName: String
        get() = "exception_mark"

    override fun attracted(tree: TreeClassifier): Boolean {
        val inserted = tree.insertedDsts
        val modifierNode = inserted.firstOrNull {
            it.type.name == "class_modifier"
                    && it.parent?.parent !in inserted
                    && it.descendants.any { it1 -> it1.label == "Throws" }
        }
        return modifierNode != null
    }
}