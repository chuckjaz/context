package dev.removingalldoubt.context.examples.mhtml

import dev.removingalldoubt.context.Applier

class Element(
    val name: String,
    val content: MutableList<Content> = mutableListOf()
) {
    override fun toString(): String =
        if (content.isEmpty()) "<$name />"
        else "<$name> ${content.joinToString(" ") {
            when (it) {
                is Content.ContentElement -> it.element.toString()
                is Content.ContentString -> it.text
            }
        }} </$name>"
}
