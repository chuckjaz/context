package dev.removingalldoubt.context.examples.mhtml

import dev.removingalldoubt.context.Applier

object ContentApplier : Applier<Content> {
    override fun insert(parent: Content, index: Int, child: Content) {
        val parentElement = (parent as Content.ContentElement).element
        parentElement.content.add(index, child)
    }

    override fun remove(parent: Content, index: Int, count: Int) {
        val parentElement = (parent as Content.ContentElement).element
        parentElement.content.subList(index, index + count).clear()
    }

    override fun move(parent: Content, from: Int, to: Int, count: Int) {
        val parentElement = (parent as Content.ContentElement).element
        val effectiveTo = if (from > to) to else to - count
        val subList = parentElement.content.subList(from, from + count)
        val contentMoving = subList.toList()
        subList.clear()
        parentElement.content.addAll(effectiveTo, contentMoving)
    }
}