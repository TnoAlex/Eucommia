package com.github.eucommia.handle

import com.github.gumtreediff.actions.TreeClassifier

@Suppress("unused")
class ExceptionMarkHandler : AbstractHandler() {
    override val handleName: String
        get() = "exception_mark"

    override fun attracted(tree: TreeClassifier): Boolean {
        return tree.insertedDsts.any {
            it.type.name == "class_modifier" &&
                    it.getChild(0).label == "@" &&
                    it.getChild("1.0.0").label == "Throws"
        }
    }
}