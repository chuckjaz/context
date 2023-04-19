package dev.removingalldoubt.context

import kotlin.coroutines.*

/**
 * [Applier] is a node adapter that allows the tree context to modify any mutable tree that can support
 * the given applier operations. A [Applier] instance should not keep its own state and should be
 * implemented as a singleton `object`. See [dev.removingalldoubt.context.examples.mhtml.ContentApplier]
 * for an example implementation.
 */
interface Applier<N> {
    /**
     * Insert [child] into [parent] at [index]. the [child] instance becomes the
     * instance at [index] and all subsequent children are moved. For example,
     * if the child list was implemented using a [MutableList] instance, [insert] can
     * be implemented by calling [MutableList.add].
     *
     * For example, [parent] had the following children `["A", "B", "C", "D"]`, then calling
     * [insert] with child of `"B.2"` with [index] of `2` would result in the list `["A", "B",
     * "B.2", "C", "D"]`. Note the new index of `"C"`, for example, is now `3` instead of `2`.
     *
     * [index] can be equal to the number of children which should be interpreted as a
     * request to append [child] to the end of the parent's child list.
     */
    fun insert(parent: N, index: Int, child: N)

    /**
     * Move [count] number of nodes from [parent] starting at [index]. All nodes after
     * `index + count` are now reduced by [count]. If [MutableList] is used to implement
     * the child list, this can be implemented by creating a sublist using
     * [MutableList.subList] from [index] to `index + count` and calling [MutableList.clear]
     * on the sublist.
     *
     * For example, if [parent] had the following children `["A", "B", "C", "D"]`, then calling
     * [remove] with [index] of 1 and [count] of 2 results in the children being, `["A", "D"]`.
     * Note that thew new index of `"D"` is `1` which is `3 - count`.
     */
    fun remove(parent: N, index: Int, count: Int)

    /**
     * Move [count] nodes at [from] to just before the element current at [to]. The position
     * of all child moved and not between [from] and [to] should be unaffected. Note that if
     * [from] is before [to] the position of [to] is likely to change when the nodes at [from]
     * are deleted and must be accounted for to ensure that the insert is before the child
     * originally at [to].
     *
     * For example, if [parent] had the following children ["A", "B", "C", "D", "E"], then
     * calling [move] with [from] of 2 and [to] of 1 and [count] of 2 should result in a
     * child list of ["A", "C", "D", "B", "E"]. Note that the index of "A" and "E" are
     * unaffected by the move.
     *
     * Calling [move] with the original list with [from] of 1 and [to] of 4 and [count] of
     * 2 should result in a child list of ["A", "D", "B", "C", "E"]. This moves "B" and "C"
     * just prior to "E". Note also that the index location of "A" and "E" are unchanged.
     *
     * For nodes that might have tree enter and tree exit notifications (such as freeing
     * resources associated with being part of the main tree), the applier should take care
     * to ensure that these notifications are not triggered by the move, if possible.
     */
    fun move(parent: N, from: Int, to: Int, count: Int)
}

/**
 * A [Processor] is represents the internal state of the tree context. The tree context is either
 * inserting, validating or updating a group. The processor uses `suspend` functions to keep track
 * of the calls to [TreeContext.startGroup], [TreeContext.startNode],
 * [TreeContext.endGroup] and [TreeContext.endNode] allowing the context to
 * be written as if [group] and [node] where called recursively instead of begin/end pairs.
 */
private abstract class Processor<N> {
    /**
     * True if content is current being inserted into the tree.
     */
    open val inserting: Boolean get() = false

    /**
     * The node for the current group if it is a node group. Will throw if the group
     * is not a node group.
     */
    open val node: N get() = error("Invalid state")

    /**
     * Enter a group. [group] and [node] are called recursively to produce a tree. A group
     * loosely correlates to a call to an extension function that will directly or
     * indirectly call [node].
     */
    abstract suspend fun group(key: Any)

    /**
     * Enter a node. [group] and [node] are called recursively to produce a tree. A node
     * represents a node that is in the resulting tree.
     */
    abstract suspend fun node()

    /**
     * Insert a node into the tree. Only valid to call when [inserting] is true and only
     * just after entering the node, before any other groups have been created. In other words,
     * the caller of [node] should immediately call [insertNode] with the node to insert
     * if the tree context is [inserting].
     */
    abstract suspend fun insertNode(n: N)

    /**
     * Called after the processor has completed with all the content for the group. Any
     * remaining groups left from the previous tree should be removed.
     */
    abstract suspend fun done()
}

/**
 * The state of a dormant tree context when it is not actively being updated.
 */
private val empty = object : Processor<Any>() {
    override suspend fun group(key: Any) = underflow()
    override suspend fun node() = underflow()
    override suspend fun insertNode(n: Any) = underflow()
    override suspend fun done() { }
    private fun underflow(): Nothing =
        error("Processor underflow")
}

@Suppress("UNCHECKED_CAST")
private fun <N> emptyProcessor(): Processor<N> = empty as Processor<N>

/**
 * An [TreeContext] context can be used to build and update an arbitrary tree that can be modified with the operations
 * defined by [Applier]. See [dev.removingalldoubt.context.examples.mhtml.element] and
 * [dev.removingalldoubt.context.examples.mhtml.text] for examples on how to use these calls as well as
 * [dev.removingalldoubt.context.examples.mhtml.Document] which the container of th HTML content updated by these
 * methods.
 */
class TreeContext<N>(root: N, val applier: Applier<N>) {
    // Public API

    fun startRoot() {
        require(processor == empty) {
            "Invalid to restart an active tree context"
        }
        processor = Validator(rootGroup, 0)
    }

    fun endRoot() {
        processor = emptyProcessor()
    }

    val inserting: Boolean get() = processor.inserting
    fun startGroup(key: Any) { continuation?.resumeWith(Result.success { processor.group(key) }) }
    fun endGroup() = end()
    fun startNode() { continuation?.resumeWith(Result.success { processor.node() }) }
    fun endNode() = end()
    fun insertNode(n: N) { continuation?.resumeWith(Result.success { processor.insertNode(n) })}
    fun useNode(): N = processor.node

    // Implementation
    private var continuation: Continuation<(suspend () -> Unit)?>? = null
    private val rootGroup = NodeGroup(null, root)

    init {
        suspend {
            content(Validator(rootGroup, 0))
        }.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext get() = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) { result.getOrThrow() }
        })
    }

    private fun end() { continuation?.resumeWith(Result.success(null)) }

    private var processor: Processor<N> = emptyProcessor()

    private inner class Inserter(
        private val parent: Group<N>,
        private var groupIndex: Int
    ): Processor<N>() {
        override val inserting: Boolean get() = true

        override suspend fun group(key: Any) {
            val group = CallGroup(parent, key)
            parent.groups.add(groupIndex++, group)
            content(Inserter(group, 0))
        }

        override suspend fun node() {
            val parentNode = parent.node ?: error("Invalid tree")
            val nodeInserter = NodeInserter(parentNode, parent.nodeIndexAt(groupIndex))
            one(nodeInserter)
            val node = nodeInserter.insertedNode
            check(node != null) { "Expected a node to be created" }
            val nodeGroup = NodeGroup(parent, node)
            parent.groups.add(groupIndex++, nodeGroup)
            content(Inserter(nodeGroup, 0))
        }

        override suspend fun done() {
            // done should remove any groups from the parent that are left over from
            // the previous state. Since this is new content, there is nothing to do here
            // as nothing could be left over.
        }

        override suspend fun insertNode(n: N) {
            error("Invalid call to insertNode")
        }
    }

    private inner class NodeInserter(val parent: N, val nodeIndex: Int): Processor<N>() {
        var insertedNode: N? = null
        override val inserting: Boolean get() = true
        override suspend fun group(key: Any) = error("Incorrect call to group")
        override suspend fun node() = error("Incorrect call to node")
        override suspend fun insertNode(n: N) {
            applier.insert(parent, nodeIndex, n)
            insertedNode = n
        }
        override suspend fun done() { }
    }

    private abstract inner class Mutator(
        val parent: Group<N>,
        var groupIndex: Int
    ) : Processor<N>() {
        override val node: N
            get() = (parent as? NodeGroup<N>)?.node ?: error("Internal error")

        override suspend fun done() {
            // If there are groups left over, delete them.
            if (groupIndex < parent.groups.size) {
                // Delete extra groups
                val nodesToDelete = parent.groups.drop(groupIndex).sumOf { it.nodeCount }
                if (nodesToDelete > 0) {
                    val nodeIndex = parent.nodeIndexAt(groupIndex)
                    val parentNode = parent.node ?: error("Internal error")
                    applier.remove(parentNode, nodeIndex, nodesToDelete)
                }
                parent.groups.subList(groupIndex, parent.groups.size).clear()
            }
        }

        override suspend fun insertNode(n: N) = error("Invalid call to insertNode")
    }

    private inner class Validator(
        parent: Group<N>,
        groupIndex: Int
    ): Mutator(parent, groupIndex) {
        override suspend fun group(key: Any) {
            val group = parent.groupAt(groupIndex)
            if (group?.key == key) {
                groupIndex++
                content(Validator(group, 0))
            } else {
                val modifier = Modifier(parent, groupIndex)
                processor = modifier
                modifier.group(key)
            }
        }

        override suspend fun node() {
            val group = parent.groupAt(groupIndex)
            if (group is NodeGroup<*>) {
                groupIndex++
                content(Validator(group, 0))
            } else {
                val modifier = Modifier(parent, groupIndex)
                processor = modifier
                modifier.node()
            }
        }
    }

    private inner class Modifier(
        parent: Group<N>,
        groupIndex: Int
    ): Mutator(parent, groupIndex) {
        override suspend fun group(key: Any) {
            // We have received a group with the given key. Find if any groups were created last
            // time with that key and move it to the current group index. If it is already at the
            // groupIndex, just validate it as moving it is not necessary. If no group is found then
            // the group is new, create an Inserter to insert its content.
            //
            // findGroupIndexAfter can be made faster by creating a hash of all the groups from the
            // initial groupIndex to the end of the group and selecting duplicates in order. This
            // implementation was chosen for simplicity, not performance.
            val index = parent.firstGroupIndexAfter(key, groupIndex)
            when {
                index == groupIndex -> {
                    // The group is in the right location, ust validate it.
                    val group = parent.groupAt(index) ?: error("Internal error")
                    groupIndex++
                    content(Validator(group, 0))
                }
                index > groupIndex -> {
                    // Move the group from its current location to this one.
                    val group = parent.groupAt(index) ?: error("Internal error")

                    // Move the nodes in the group
                    val parentNode = parent.node ?: error("Internal error")
                    val sourceIndex = parent.nodeIndexAt(index)
                    val destinationIndex = parent.nodeIndexAt(groupIndex)
                    applier.move(
                        parent = parentNode,
                        from = sourceIndex,
                        to = destinationIndex,
                        count = group.nodeCount
                    )

                    // Move the group to the current node group
                    parent.groups.removeAt(index)
                    parent.groups.add(groupIndex, group)
                    groupIndex++

                    // Validate the group
                    content(Validator(group, 0))
                }
                else -> {
                    // This is a new group, insert it into the parent and insert its content
                    // using an Inserter.
                    val callGroup = CallGroup(parent, key)
                    parent.groups.add(groupIndex, callGroup)
                    content(Inserter(callGroup, 0))
                    groupIndex++
                }
            }
        }

        override suspend fun node() {
            // The code in group() can be refactored lift the requirement below at the expense
            // of making it a bit more complicated.
            error("For simplicity, node groups cannot be inserted directly")
        }

        override suspend fun insertNode(n: N) = error("Incorrect call to insertNode")
    }

    /**
     * [content] is the equivalent of [kotlin.sequences.SequenceScope.yield] and allows the corresponding
     * context function to resume executing.
     */
    private suspend fun content(processor: Processor<N>) {
        val previousProcessor = this.processor
        this.processor = processor
        try {
            while (true) {
                val result = suspendCoroutine { continuation = it }
                result?.invoke() ?: return
            }
        } finally {
            this.processor.done()
            this.processor = previousProcessor
        }
    }

    /**
     * Similar to [content] but expects just one call to one a processor method instead of all calls
     * until a call to [end].
     */
    private suspend fun one(processor: Processor<N>) {
        val previousProcessor = this.processor
        this.processor = processor
        try {
            val result = suspendCoroutine { continuation = it }
            result?.invoke()
        } finally {
            this.processor.done()
            this.processor = previousProcessor
        }
    }
}

/**
 * Create a group with a user defined key. This allows the tree context to determine when content
 * has moved.
 */
inline fun <N> Context<TreeContext<N>>.key(
    key: Any, crossinline content: Context<TreeContext<N>>.() -> Unit
) {
    context.startGroup(key)
    content()
    context.endGroup()
}

/**
 * A [Group] maintains the data necessary to update the tree. This data is kept in sync
 * with the tree each time the tree is updated.
 */
private abstract class Group<N>(val parent: Group<N>?, val key: Any) {
    open val node: N? get() = parent?.node
    val groups = mutableListOf<Group<N>>()

    open val nodeCount: Int get() = groups.sumOf { it.nodeCount }
    fun groupAt(index: Int): Group<N>? = if (index < groups.size) groups[index] else null
    fun firstGroupIndexAfter(key: Any, onOrAfter: Int): Int {
        for (index in onOrAfter until groups.size) {
            val group = groups[index]
            if (group.key == key) return index
        }
        return -1
    }

    abstract fun nodeIndexOf(child: Group<N>): Int

    fun nodeIndexAt(index: Int): Int {
        return if (index < groups.size) {
            nodeIndexOf(groups[index])
        } else {
            // Passing this will give the total nodes in this
            nodeIndexOf(this)
        }
    }

    protected fun nodesUpTo(child: Group<N>): Int {
        var result = 0
        for (group in groups) {
            if (child == group) return result
            result += group.nodeCount
        }
        return result
    }
}

/**
 * A [CallGroup] tracks calls to group().
 */
private class CallGroup<N>(parent: Group<N>?, key: Any): Group<N>(parent, key) {
    override fun nodeIndexOf(child: Group<N>): Int {
        val myIndex = parent?.nodeIndexOf(this) ?: 0
        return myIndex + nodesUpTo(child)
    }
}

/**
 * A [NodeGroup] tracks calls to node().
 */
private class NodeGroup<N>(parent: Group<N>?, override val node: N): Group<N>(parent, NodeKey) {
    override val nodeCount: Int get() = 1
    override fun nodeIndexOf(child: Group<N>): Int = nodesUpTo(child)
}

private object NodeKey