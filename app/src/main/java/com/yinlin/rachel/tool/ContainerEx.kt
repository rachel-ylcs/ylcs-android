package com.yinlin.rachel.tool

fun <E> MutableCollection<E>.clearAddAll(element: Collection<E>) {
    clear()
    addAll(element)
}

fun <K, V> MutableMap<K, V>.clearAddAll(element: Map<out K, V>) {
    clear()
    putAll(element)
}

fun <T> MutableList<T>.moveItem(fromIndex: Int, toIndex: Int) {
    if (fromIndex in 0..<this.size && toIndex in 0..< this.size) {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}