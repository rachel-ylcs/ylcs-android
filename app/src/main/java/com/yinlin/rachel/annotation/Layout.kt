package com.yinlin.rachel.annotation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Layout(val cls: KClass<out ViewBinding>) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> Layout.inflate(inflater: LayoutInflater, parent: ViewGroup?): T {
            val method = cls.java.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.javaPrimitiveType)
            return method.invoke(null, inflater, parent, false) as T
        }
    }
}