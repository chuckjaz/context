package dev.removingalldoubt.context.examples.html

data class Element(
    val name: String,
    val content: List<Content> = emptyList()
) {
    fun withContent(element: Element) =
        Element(name, content + Content.ContentElement(element))
    fun withContent(text: String) =
        Element(name,  content + Content.ContentString(text))

    override fun toString(): String =
        if (content.isEmpty()) "<$name />"
        else "<$name> ${content.joinToString(" ") { 
            when (it) {
                is Content.ContentElement -> it.element.toString()
                is Content.ContentString -> it.text
            }
        }} </$name>"
}