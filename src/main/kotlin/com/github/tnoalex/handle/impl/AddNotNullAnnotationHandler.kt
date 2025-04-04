package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class AddNotNullAnnotationHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_not_null_annotation"

    override fun attracted(tree: TreeClassifier): Boolean {
        val inserted = tree.insertedDsts
        val node = inserted.firstOrNull {
            it.type.name == "marker_annotation" &&
                    it.children.find { c -> c.label == "NotNull" || c.label == "NonNull" } != null
                    && it.parent !in inserted
        }
        return node != null
    }
}