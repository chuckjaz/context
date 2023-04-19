package dev.removingalldoubt.context.examples.io

import dev.removingalldoubt.context.Context
import dev.removingalldoubt.context.context

/**
 * [TextFile] is a simulation of reading text from storage line by line. [current] represents which line will be
 * read by calling [readLine].
 *
 * This object can be used as a text monad by creating a new version of [TextFile] that is the state of the text file
 * after it is read or, if the reference to the [TextFile] instance is unique, then [readLineUnique] can be used that
 * will update the value of [current] in place (intended for use with  uniqueness typing).
 */
class TextFile(private val lines: List<String>, private var current: Int = 0) {
    // Monad pattern
    fun eof(): Pair<Boolean, TextFile> = (current >= lines.size) to this
    fun readLine(): Pair<String, TextFile> = lines[current] to TextFile(lines, current + 1)

    // Unique pattern
    fun eofUnique() = current >= lines.size
    fun readLineUnique() = lines[current++]
}

fun Context<TextFile>.eof() = e { eof() }
fun Context<TextFile>.readLine() = e { readLine() }
fun Context<TextFile>.eofUnique() = this.context.eofUnique()
fun Context<TextFile>.readLineUnique() = this.context.readLineUnique()
/**
 * [readAll] uses the monad usage pattern of [TextFile] using [context] to implement a monad expression similar to
 * the `do` expression in Haskell, for example. Calls to [Context.e] can be interpreted similar to a `lift` in the
 * in a `do` expression.
 */
fun TextFile.readAll(): Pair<List<String>, TextFile>
        = context(this) {
    val result = mutableListOf<String>()
    while( !e { eof() }) {
        result += e { readLine() }
    }
    result
}

/**
 * [readAll] uses the monad usage pattern of [TextFile] using [context] to implement a monad expression similar to
 * the `do` expression in Haskell, using context receiver extensions to implicitly lift the monad.
 */
fun Context<TextFile>.readAll(): List<String> {
    val result = mutableListOf<String>()
    while (!eof()) {
        result += readLine()
    }
    return result
}

/**
 * [readAll] implemented assuming a unique context
 */
fun Context<TextFile>.readAllUnique(): List<String> {
    val result = mutableListOf<String>()
    while (!eofUnique()) {
        result += readLineUnique()
    }
    return result
}

/**
 * [readAll] implemented directly assuming unique [this]
 */
fun TextFile.readAllUnique(): List<String> {
    val result = mutableListOf<String>()
    while (!eofUnique()) {
        result += readLineUnique()
    }
    return result
}

val data = listOf("a", "b", "c", "d", "e")
val file = TextFile(data)

fun example_monadic_explicit() {
    val result = context(file) { e { readAll() } }.first
    println("Monadic explicit: $result")
}

fun example_monadic_implicit() {
    val result = context(file) { readAll() }.first
    println("Monadic implicit: $result")
}

fun example_context_unique() {
    val result = context(TextFile(data)) { readAllUnique() }.first
    println("Unique context: $result")
}

fun example_context_direct() {
    val result = TextFile(data).readAllUnique()
    println("Unique direct: $result")
}

fun ioExamples() {
    example_monadic_explicit()
    example_monadic_implicit()
    example_context_unique()
    example_context_direct()
}