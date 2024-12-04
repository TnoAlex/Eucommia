package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler


@ConstructByReflection
class NullSafeModifierHandler : AbstractHandler() {
    override val handleName: String
        get() = "add_null_able_annotation"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "marker_annotation" && it.children.find { c -> c.label == "Nullable" } != null
        }
    }
}