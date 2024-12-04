package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class AddNullSafeOperatorHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_safe_operator"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "null_safe_call_operator"
        }
    }
}