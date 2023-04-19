package dev.removingalldoubt.context.examples.mhtml


@PublishedApi
internal fun Html._startElement(key: Any, name: String) {
    context.startGroup(key)
    context.startNode()
    if (context.inserting) {
        val element = Element(name)
        context.insertNode(Content.ContentElement(element))
    }
}

@PublishedApi
internal fun Html._endElement() {
    context.endNode()
    context.endGroup()
}

inline fun Html.element(key: Any, name: String, crossinline content: Html.() -> Unit) {
    _startElement(key, name)
    content()
    _endElement()
}

@PublishedApi
internal class Key(private val name: String) {
    override fun toString(): String = "key:$name"
}

@PublishedApi
internal val pKey = Key("p")
inline fun Html.p(crossinline content: Html.() -> Unit) {
    element(pKey, "p", content)
}

@PublishedApi
internal val divKey = Key("div")
inline fun Html.div(crossinline content: Html.() -> Unit) {
    element(divKey, "div", content)
}

@PublishedApi
internal val spanKey = Key("span")
inline fun Html.span(crossinline content: Html.() -> Unit) {
    element(spanKey, "span", content)
}

private val textKey = Key("test")
fun Html.text(text: String) {
    context.startGroup(textKey)
    context.startNode()
    if (context.inserting) {
        context.insertNode(Content.ContentString(text))
    } else {
        val content = context.useNode() as Content.ContentString
        content.text = text
    }
    context.endNode()
    context.endGroup()
}

fun Html.group(key: Any, content: Html.() -> Unit) {
    context.startGroup(key)
    content()
    context.endGroup()
}

fun document(content: Html.() -> Unit): Document {
    val result = Document()
    result.setContent(content)
    return result
}