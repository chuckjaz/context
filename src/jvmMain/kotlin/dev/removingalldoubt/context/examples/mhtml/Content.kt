package dev.removingalldoubt.context.examples.mhtml

sealed class Content {
    data class ContentElement(val element: Element): Content()
    data class ContentString(var text: String): Content()
}