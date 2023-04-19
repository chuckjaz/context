package dev.removingalldoubt.context

@DslMarker
annotation class ContextMarker

/**
 * [Context] is a receiver scope that introduces an implied context that is updated with the result of
 * calling the lambda passed to [e], which is short for "execute" as it executes the lambda and updates
 * the corresponding context. If the function does not have a result (other than the new context) then
 * [u] can be used instead of calling [e] with a unit returning function to avoid the creation of a needless
 * [Pair].
 */
@ContextMarker
class Context<T>(context: T) {
    private var _context = context
    val context: T get() = _context

    /**
     * Execute [f] in the context of the current instance of [T]. The [T] returned (the second part of the pair
     * returned by [f]) is then the new context for subsequent calls to either [e] or [u]. The result of [e] is the
     * first part of the pair.
     */
    fun <R> e(f: T.() -> Pair<R, T>): R {
        val (r, c) = _context.f()
        _context = c
        return r
    }

    /**
     * Execute [f] in the context of the current instance of [T]. The [T] returned is then the new context for
     * subsequent calls to either [e] or [u]. [u] should be used instead of [e] when the [f] has no return result
     * other than the transformed [T].
     */
    fun u(f: T.() -> T) { _context = _context.f() }
}

/**
 * [context] is a helper function around [Context] that introduces a scope where an instance of [Context] is
 * created and made the receiver scope of [block]. All invocations of [Context.e] and [Context.u] in the block update
 *  * the context to the context of type [T] that is a result of the calling their lambda parameter.
 */
inline fun <T, R> context(context: T, block: Context<T>.() -> R): Pair<R, T> {
    val c = Context(context)
    val r = c.block()
    return r to c.context
}

