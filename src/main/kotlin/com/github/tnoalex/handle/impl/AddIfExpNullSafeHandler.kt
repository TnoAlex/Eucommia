package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler
import com.github.tnoalex.utils.ifFalse

@ConstructByReflection
class AddIfExpNullSafeHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_if_exp_for_null_safe"

    override fun attracted(tree: TreeClassifier): Boolean {
        val inserted = tree.insertedDsts
        val node =  inserted.firstOrNull {
            val condition = it.children.firstOrNull { c -> c.type.name == "equality_expression" }
                ?: return@firstOrNull false
            condition.children.any { c -> c.label == "!=" }.ifFalse { return@firstOrNull false }
            condition.children.any { c -> c.type.name == "null" }.ifFalse { return@firstOrNull false }
            it.parent !in inserted
        }
        return node != null
    }
}