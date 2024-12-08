package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class NonNull2NullableHandler: AbstractHandler() {
    override val handleName: String
        get() = "non_null_2_nullable"

    override fun attracted(tree: TreeClassifier): Boolean {
        val inserted = tree.insertedDsts
        val node = inserted.firstOrNull {
            it.type.name == "nullable_type"
        }
        return node != null && node.parent !in inserted
    }
}