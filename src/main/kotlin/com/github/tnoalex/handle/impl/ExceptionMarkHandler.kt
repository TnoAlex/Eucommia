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
        }
        val throwsNode = inserted.firstOrNull {
            it.label == "Throws"
        }
        return throwsNode != null && modifierNode != null && modifierNode.parent?.parent !in inserted
                && throwsNode in modifierNode.descendants
    }
}