@file:Suppress("unused")

package com.github.tnoalex.handle

import com.github.tnoalex.utils.ifFalse
import com.github.gumtreediff.actions.TreeClassifier

class AddNullSafeOperatorHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_safe_operator"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "null_safe_call_operator"
        }
    }
}

class AddNullAssertionOperatorHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_assertion_operator"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "non-null_assertion_operator"
        }
    }
}

class AddIfExpNullSafeHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_if_exp_for_null_safe"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            val condition = it.children.firstOrNull { c -> c.type.name == "equality_expression" } ?: return false
            condition.children.any { c -> c.label == "!=" }.ifFalse { return false }
            condition.children.any { c -> c.type.name == "null" }.ifFalse { return false }
            true
        }
    }
}

class AddNotNullAnnotationHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_not_null_annotation"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "marker_annotation" &&
                    it.children.find { c -> c.label == "NotNull" || c.label == "NonNull" } != null
        }
    }
}


class AddNullAbleAnnotationHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_able_annotation"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "marker_annotation" && it.children.find { c -> c.label == "Nullable" } != null
        }
    }
}