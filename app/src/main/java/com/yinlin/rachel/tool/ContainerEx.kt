package com.yinlin.rachel.tool

fun <E> MutableCollection<E>.clearAddAll(element: Collection<E>) {
    clear()
    addAll(element)
}

fun <K, V> MutableMap<K, V>.clearAddAll(element: Map<out K, V>) {
    clear()
    putAll(element)
}