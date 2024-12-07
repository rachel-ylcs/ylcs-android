package com.yinlin.rachel.model

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.viewbinding.ViewBinding
import com.google.android.material.card.MaterialCardView
import com.yinlin.rachel.R
import com.yinlin.rachel.databinding.DialogChoiceBinding
import com.yinlin.rachel.databinding.DialogConfirmBinding
import com.yinlin.rachel.databinding.DialogInfoBinding
import com.yinlin.rachel.databinding.DialogInputBinding
import com.yinlin.rachel.databinding.DialogLoadingBinding
import com.yinlin.rachel.databinding.DialogProgressBinding
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.toDP
import com.yinlin.rachel.tool.visible
import com.yinlin.rachel.view.InputView

abstract class RachelDialog<T : ViewBinding> (
    private val context: Context,
    private val layout: Class<T>,
    private val cancelable: Boolean = true,
    private val fixedWidth: Boolean = true,
    private val padding: Int = 10.toDP(context),
    private val radius: Float = 10f.toDP(context),
) : Dialog(context, R.style.RachelDialog) {
    private var _binding: T? = null
    protected val v: T get() = _binding!!
    
    protected open fun init(v: T) { }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.dialog_base)
        window?.apply {
            val display = context.resources.displayMetrics
            val attr = attributes
            attr.gravity = Gravity.CENTER
            attr.y -= 60.toDP(context)
            if (fixedWidth) attr.width = (display.widthPixels * 0.9f).toInt()
            attr.height = (display.heightPixels * 0.75f).toInt()
            attributes = attr
        }
        val method = layout.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.javaPrimitiveType)
        val container = findViewById<LinearLayout>(R.id.container)
        val card = findViewById<MaterialCardView>(R.id.card)
        card.setContentPadding(padding, padding, padding, padding)
        card.radius = radius
        if (fixedWidth) {
            var params = container.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            container.layoutParams = params
            params = card.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            card.layoutParams = params
        }
        _binding = method.invoke(null, LayoutInflater.from(context), card, true) as T
        setCanceledOnTouchOutside(false)
        setCancelable(cancelable)
        setOnDismissListener { _binding = null }
        init(v)
    }

    companion object {
        fun info(context: Context, title: String = "提示", content: String) {
            object : RachelDialog<DialogInfoBinding>(context, DialogInfoBinding::class.java) {
                override fun init(v: DialogInfoBinding) {
                    v.title.text = title
                    v.content.text = content
                    v.ok.rachelClick { dismiss() }
                }
            }.show()
        }

        fun confirm(context: Context, title: String = "二次确认", content: String, callback: () -> Unit) {
            object : RachelDialog<DialogConfirmBinding>(context, DialogConfirmBinding::class.java) {
                override fun init(v: DialogConfirmBinding) {
                    v.title.text = title
                    v.content.text = content
                    v.ok.rachelClick {
                        dismiss()
                        callback()
                    }
                    v.cancel.rachelClick { dismiss() }
                }
            }.show()
        }

        fun input(context: Context, content: String, maxLength: Int = 0, maxLine: Int = 1, inputType: Int = InputType.TYPE_CLASS_TEXT, callback: (String) -> Unit) {
            object : RachelDialog<DialogInputBinding>(context, DialogInputBinding::class.java, false) {
                override fun init(v: DialogInputBinding) {
                    v.input.hint = content
                    v.input.inputType = inputType
                    v.input.maxLength = maxLength
                    v.input.maxLines = maxLine.coerceAtMost(10)
                    if (maxLength > 0) v.input.overflowListener = InputView.OverFlowListener { v.ok.isEnabled = !it }
                    v.ok.rachelClick {
                        dismiss()
                        callback(v.input.text)
                    }
                    v.cancel.rachelClick { dismiss() }
                }
            }.show()
        }

        fun choice(context: Context, title: String = "", items: List<String>, callback: (Int) -> Unit) {
            object : RachelDialog<DialogChoiceBinding>(context, DialogChoiceBinding::class.java) {
                override fun init(v: DialogChoiceBinding) {
                    if (title.isEmpty()) v.title.visible = false
                    else v.title.text = title
                    v.list.apply {
                        adapter = ArrayAdapter(context, R.layout.item_choice, items)
                        onItemClickListener = OnItemClickListener { _, _, position, _ ->
                            dismiss()
                            callback(position)
                        }
                    }
                }
            }.show()
        }

        fun choice(context: Context, title: String = "", callbacks: List<Pair<String, () -> Unit>>) {
            object : RachelDialog<DialogChoiceBinding>(context, DialogChoiceBinding::class.java) {
                override fun init(v: DialogChoiceBinding) {
                    if (title.isEmpty()) v.title.visible = false
                    else v.title.text = title
                    v.list.apply {
                        adapter = ArrayAdapter(context, R.layout.item_choice, callbacks.map { it.first })
                        onItemClickListener = OnItemClickListener { _, _, position, _ ->
                            dismiss()
                            callbacks.getOrNull(position)?.second?.invoke()
                        }
                    }
                }
            }.show()
        }

        class DialogProgress(context: Context, private val t: String) : RachelDialog<DialogProgressBinding>(context, DialogProgressBinding::class.java, false) {
            var title: String
                get() = v.title.text.toString()
                set(value) { v.title.text = value }
            var progress: Int
                get() = v.progress.progress
                set(value) {
                    v.progress.progress = value
                    v.tvProgress.text = "${value * 100 / v.progress.max}%"
                }
            var maxProgress: Int
                get() = v.progress.max
                set(value) {
                    if (value > 0) {
                        v.progress.max = value
                        v.tvProgress.text = "${v.progress.progress * 100 / value} %"
                    }
                }
            var isCancel: Boolean = false

            override fun init(v: DialogProgressBinding) {
                title = t
                v.cancel.rachelClick {
                    isCancel = true
                    v.cancel.isEnabled = false
                }
            }
        }

        fun progress(context: Context, title: String = ""): DialogProgress {
            val dialog = DialogProgress(context, title)
            dialog.show()
            return dialog
        }

        class DialogLoading(context: Context, private val handler: Handler, private val title: String) : RachelDialog<DialogLoadingBinding>(context, DialogLoadingBinding::class.java, false, false) {
            // 加载等待框 防抖设计
            // 延迟时间与显示时间默认为 500毫秒
            companion object {
                const val MIN_DELAY = 500L
                const val MIN_SHOW = 500L
            }

            private var startTime = -1L
            private var postedHide = false
            private var postedShow = false
            private var isDismiss = false

            private val delayHide = Runnable {
                postedHide = false
                startTime = -1
                super.dismiss()
            }

            private val delayShow = Runnable {
                postedShow = false
                if (!isDismiss) {
                    startTime = System.currentTimeMillis()
                    super.show()
                }
            }

            override fun init(v: DialogLoadingBinding) {
                v.title.text = title
                v.pic.load(R.drawable.img_dialog_loading_rachel)
            }

            override fun show() {
                startTime = -1
                isDismiss = false
                handler.removeCallbacks(delayHide)
                postedHide = false
                if (!postedShow) {
                    handler.postDelayed(delayShow, MIN_DELAY)
                    postedShow = true
                }
            }

            override fun dismiss() {
                isDismiss = true
                handler.removeCallbacks(delayShow)
                postedShow = false
                val diff = System.currentTimeMillis() - startTime
                if (diff >= MIN_SHOW || startTime == -1L) super.dismiss()
                else if (!postedHide) {
                    handler.postDelayed(delayHide, MIN_SHOW - diff)
                    postedHide = true
                }
            }
        }

        fun loading(context: Context, handler: Handler, title: String = "正在加载中..."): DialogLoading {
            val dialog = DialogLoading(context, handler, title)
            dialog.show()
            return dialog
        }
    }
}