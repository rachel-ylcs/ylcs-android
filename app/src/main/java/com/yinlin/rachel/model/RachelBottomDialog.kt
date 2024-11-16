package com.yinlin.rachel.model

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yinlin.rachel.R

abstract class RachelBottomDialog<Binding : ViewBinding, F : RachelFragment<*>>
    (protected val root: F, maxHeightPercent: Float, cls: Class<Binding>) {
    val context = root.pages.context
    val v: Binding
    private var dialog: BottomSheetDialog? = null
    private val maxDialogHeight = (context.resources.displayMetrics.heightPixels * maxHeightPercent).toInt()

    init {
        val method = cls.getMethod("inflate", LayoutInflater::class.java)
        @Suppress("UNCHECKED_CAST")
        v = method.invoke(null, LayoutInflater.from(context)) as Binding
        v.root.setBackgroundResource(R.drawable.bg_dialog_white)
    }

    protected open fun init() { }
    protected open fun quit() { }

    fun show() {
        dialog = BottomSheetDialog(context, R.style.Theme_RachelBottomDialog)
        dialog?.setContentView(v.root)
        dialog?.setOnDismissListener {
            (v.root.parent as ViewGroup).removeView(v.root)
            dialog = null
        }
        dialog?.behavior?.apply {
            maxHeight = maxDialogHeight
            peekHeight = maxDialogHeight
        }
        dialog?.show()
    }

    fun hide() {
        dialog?.dismiss()
    }

    fun release() {
        dialog?.dismiss()
        quit()
    }

    val lifecycleScope: LifecycleCoroutineScope get() = root.lifecycleScope
}