package com.yinlin.rachel.model

import android.os.Bundle
import android.view.LayoutInflater
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.SheetLayout
import com.yinlin.rachel.annotation.SheetLayout.Companion.inflate
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.tool.meta

abstract class RachelSheet<Binding : ViewBinding, F : RachelFragment<*>>(protected val fragment: F)
    : BottomSheetDialog(fragment.main, R.style.Theme_RachelBottomDialog) {
    private var _binding: Binding? = null
    val v get() = _binding!!

    protected open fun init() { }
    protected open fun quit() { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutMeta = this.meta<SheetLayout>()!!
        behavior.apply {
            val maxDialogHeight = (context.resources.displayMetrics.heightPixels * layoutMeta.percent).toInt()
            maxHeight = maxDialogHeight
            peekHeight = maxDialogHeight
        }

        _binding = layoutMeta.inflate(LayoutInflater.from(context))
        val root = v.root
        root.setBackgroundResource(R.drawable.bg_sheet_white)
        setContentView(root)
        init()

        setOnDismissListener {
            quit()
            _binding = null
        }
    }

    fun tip(type: Tip, text: String) = fragment.tip(type, text, window?.decorView)
}