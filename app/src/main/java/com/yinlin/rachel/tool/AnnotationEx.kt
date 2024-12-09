package com.yinlin.rachel.tool


inline fun <reified T : Annotation> Any.meta(): T? {
    for (annotation in this::class.annotations) {
        if (annotation is T) return annotation
    }
    return null
}

inline fun <reified T : Annotation> Any.metas(): List<T> {
    val data = mutableListOf<T>()
    for (annotation in this::class.annotations) {
        if (annotation is T) data += annotation
    }
    return data
}