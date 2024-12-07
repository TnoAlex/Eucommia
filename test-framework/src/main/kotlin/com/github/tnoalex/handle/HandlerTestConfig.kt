package com.github.tnoalex.handle

data class HandlerTestConfig(
    val oldName: String,
    val newName: String,
    /**
     * handler names here. see [com.github.tnoalex.handle.AbstractHandler.handleName]
     */
    val result: List<String>
)
