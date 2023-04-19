package dev.removingalldoubt.context.examples.html

import dev.removingalldoubt.context.Context
import dev.removingalldoubt.context.context

private inline fun Element.withContent(
    name: String,
    content: Context<Element>.() -> Unit
): Element = withContent(context(Element(name), content).second)

fun Context<Element>.element(name: String, content: Context<Element>.() -> Unit) =
    u { withContent(name, content) }
fun Context<Element>.text(text: String) =
    u { withContent(text) }

fun Context<Element>.head(content: Context<Element>.() -> Unit) =
    element("head", content)
fun Context<Element>.body(content: Context<Element>.() -> Unit) =
    element("body", content)
fun Context<Element>.title(content: Context<Element>.() -> Unit) =
    element("title", content)
fun Context<Element>.p(content: Context<Element>.() -> Unit) =
    element("p", content)

fun html(content: Context<Element>.() -> Unit): Element =
    context(Element("html"), content).second

fun htmlExample() {
    val document = html {
        head {
            title {
                text("Document title")
            }
        }
        body {
            p {
                text("Outer paragraph")
                p {
                    text("Inner paragraph")
                }
            }
        }
    }
    println("Document: $document")
}