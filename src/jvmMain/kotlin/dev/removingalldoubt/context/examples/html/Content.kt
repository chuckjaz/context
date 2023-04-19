package dev.removingalldoubt.context.examples.html

sealed class Content {
    data class ContentElement(val element: Element): Content() {
        override fun toString(): String = element.toString()
    }
    data class ContentString(val text: String): Content() {
        override fun toString() = text
    }
}