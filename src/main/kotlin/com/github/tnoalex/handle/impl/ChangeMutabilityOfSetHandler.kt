package com.github.tnoalex.handle.impl

import com.github.gumtreediff.actions.TreeClassifier
import com.github.tnoalex.ConstructByReflection
import com.github.tnoalex.handle.AbstractHandler

@ConstructByReflection
class ChangeMutabilityOfSetHandler : AbstractHandler() {
    override val handleName: String
        get() = "change_mutability_of_set"

    override fun attracted(tree: TreeClassifier): Boolean {
        val updatedDsts = tree.updatedDsts
        val updatedSrcs = tree.updatedSrcs
        val nodeDst = updatedDsts.firstOrNull {
            it.type.name == "type_identifier" && it.label.startsWith("Mutable")
                    && it.parent !in updatedDsts
        }
        val nodeSrc = updatedSrcs.firstOrNull {
            it.type.name == "type_identifier" && it.label.startsWith("Mutable")
                    && it.parent !in updatedSrcs
        }
        return nodeDst != null || nodeSrc != null
    }
}