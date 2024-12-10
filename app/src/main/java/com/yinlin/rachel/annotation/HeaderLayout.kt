package com.yinlin.rachel.annotation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HeaderLayout(val headerCls: KClass<out ViewBinding>, val itemCls: KClass<out ViewBinding>) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun <T> HeaderLayout.inflate(cls: KClass<out ViewBinding>, inflater: LayoutInflater, parent: ViewGroup?): T {
            val method = cls.java.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.javaPrimitiveType)
            return method.invoke(null, inflater, parent, false) as T
        }

        fun <T> HeaderLayout.inflateHeader(inflater: LayoutInflater, parent: ViewGroup?): T = inflate(headerCls, inflater, parent)
        fun <T> HeaderLayout.inflateItem(inflater: LayoutInflater, parent: ViewGroup?): T = inflate(itemCls, inflater, parent)
    }
}