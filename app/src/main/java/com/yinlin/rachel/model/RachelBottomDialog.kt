package com.yinlin.rachel.model

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip

abstract class RachelBottomDialog<Binding : ViewBinding, F : RachelFragment<*>>
    (protected val root: F, maxHeightPercent: Float, cls: Class<Binding>) {
    val context: Context = root.main
    val v: Binding
    private var dialog: BottomSheetDialog? = null
    private val maxDialogHeight = (context.resources.displayMetrics.heightPixels * maxHeightPercent).toInt()
    private var isInit = false

    init {
        val method = cls.getMethod("inflate", LayoutInflater::class.java)
        @Suppress("UNCHECKED_CAST")
        v = method.invoke(null, LayoutInflater.from(context)) as Binding
        v.root.setBackgroundResource(R.drawable.bg_dialog_white)
    }

    protected open fun init() { }
    protected open fun hidden() { }
    protected open fun quit() { }

    fun show() {
        if (!isInit) {
            isInit = true
            init()
        }
        dialog = BottomSheetDialog(context, R.style.Theme_RachelBottomDialog)
        dialog?.setContentView(v.root)
        dialog?.setOnDismissListener {
            hidden()
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
        if (isInit) quit()
    }

    val lifecycleScope: LifecycleCoroutineScope get() = root.lifecycleScope

    fun tip(type: Tip, text: String) = root.tip(type, text, dialog?.window?.decorView)
}