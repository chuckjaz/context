package dev.removingalldoubt.context.examples.mhtml

import androidx.compose.runtime.snapshots.Snapshot
import dev.removingalldoubt.context.Context
import dev.removingalldoubt.context.TreeContext

class Document {
    private val root = Content.ContentElement(Element("body"))
    private var observed: Set<Any> = emptySet()
    private val treeContext = TreeContext(root, ContentApplier)
    private var invalid = false
    private val unregisterObserver = Snapshot.registerApplyObserver { changes, _ ->
        invalid = invalid || changes.any { it in observed }
    }
    private var content: Html.() -> Unit = { }

    fun setContent(content: Html.() -> Unit) {
        val context = Context(treeContext)
        this.content = {
            this.context.startRoot()
            content()
            this.context.endRoot()
        }
        observe {
            val thisContent = this.content
            context.thisContent()
        }
    }

    fun update() {
        if (invalid) {
            val context = Context(treeContext)
            val content = this.content
            observe {
                context.content()
            }
            invalid = false
        }
    }

    override fun toString(): String = root.toString()

    fun dispose() {
        unregisterObserver.dispose()
    }

    private fun observe(block: () -> Unit) {
        val newObserved = mutableSetOf<Any>()
        val snapshot = Snapshot.takeMutableSnapshot(readObserver = { newObserved.add(it) } )
        try {
            snapshot.enter(block)
            snapshot.apply().check()
        } finally {
            snapshot.dispose()
        }
        this.observed = newObserved
    }
}

typealias Html = Context<TreeContext<Content>>

