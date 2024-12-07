package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class AddNullAssertionOperatorHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_assertion_operator"

    override fun attracted(tree: TreeClassifier): Boolean {
        val inserted = tree.insertedDsts
        val node = inserted.firstOrNull {
            it.type.name == "non-null_assertion_operator"
        }
        // non-null assertion will change the expressions type, so a postfix expression is also inserted here.
        // So we use node.parent?.parent here.
        return node != null && node.parent?.parent !in inserted
    }
}