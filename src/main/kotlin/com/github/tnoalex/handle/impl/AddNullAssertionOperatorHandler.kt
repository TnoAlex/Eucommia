package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class AddNullAssertionOperatorHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_assertion_operator"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "non-null_assertion_operator"
        }
    }
}