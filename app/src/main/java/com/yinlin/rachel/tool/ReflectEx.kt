package com.yinlin.rachel.tool

import java.lang.reflect.Field

fun <T> Any.reflect(name: String): T? = try {
    val field: Field = this.javaClass.getDeclaredField(name)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    field.get(this) as T
}
catch (_: Exception) { null }