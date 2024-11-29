package com.yinlin.rachel.model

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.Tip

abstract class RachelSheet<Binding : ViewBinding, F : RachelFragment<*>>(
    protected val fragment: F,
    maxHeightPercent: Float
) : BottomSheetDialog(fragment.main, R.style.Theme_RachelBottomDialog) {
    private val maxDialogHeight = (context.resources.displayMetrics.heightPixels * maxHeightPercent).toInt()

    private var _binding: Binding? = null
    val v get() = _binding!!

    protected open fun init() { }
    protected open fun quit() { }

    protected abstract fun bindingClass(): Class<Binding>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        behavior.apply {
            maxHeight = maxDialogHeight
            peekHeight = maxDialogHeight
        }

        val cls = bindingClass()
        val method = cls.getMethod("inflate", LayoutInflater::class.java)
        @Suppress("UNCHECKED_CAST")
        _binding = method.invoke(null, LayoutInflater.from(context)) as Binding
        val root = v.root
        root.setBackgroundResource(R.drawable.bg_sheet_white)
        setContentView(root)
        init()

        setOnDismissListener {
            quit()
            _binding = null
        }
    }

    val lifecycleScope: LifecycleCoroutineScope get() = fragment.lifecycleScope

    fun tip(type: Tip, text: String) = fragment.tip(type, text, window?.decorView)
}