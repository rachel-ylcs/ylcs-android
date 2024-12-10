package com.yinlin.rachel.annotation

import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SheetLayout(val cls: KClass<out ViewBinding>, val percent: Float) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> SheetLayout.inflate(inflater: LayoutInflater): T {
            val method = cls.java.getMethod("inflate", LayoutInflater::class.java)
            return method.invoke(null, inflater) as T
        }
    }
}