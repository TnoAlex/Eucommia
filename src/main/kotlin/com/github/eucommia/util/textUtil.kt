package com.github.eucommia.util

private val KT_COMMENTS_REGEX = Regex("(?<!:)//.*|/\\*(\\s|.)*?\\*/")

fun removeComments(text: String): String {
    return text.replace(KT_COMMENTS_REGEX, "")
}