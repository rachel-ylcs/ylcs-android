package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.ri
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.textSizePx
import com.yinlin.rachel.tool.visible

class ButtonGroup @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {
    companion object {
        const val BOTTOM = 1
        const val TOP = 2

        const val WRAP = 1
        const val AVERAGE = 2
    }

    private val items = mutableListOf<TextView>()

    var listener: ((Int) -> Unit)? = null

    init {
        RachelAttr(context, attrs, R.styleable.ButtonGroup).use {
            val textPosition = it.value(R.styleable.ButtonGroup_ButtonGroupTextPosition, BOTTOM)
            val itemGravity = it.value(R.styleable.ButtonGroup_ButtonGroupItemGravity, WRAP)
            val itemGap = it.dp(R.styleable.ButtonGroup_ButtonGroupItemGap, 5)
            val splitStrings = it.value(R.styleable.ButtonGroup_ButtonGroupText, "").split(",")
            val textSize = it.spx(R.styleable.ButtonGroup_android_textSize, R.dimen.sm)
            val textColor = it.color(R.styleable.ButtonGroup_android_color, R.color.black)
            val textBold = it.value(R.styleable.ButtonGroup_Bold, false)
            val gap = it.dp(R.styleable.ButtonGroup_Gap, 5)
            val iconsArray = arrayOf(
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon1, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon2, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon3, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon4, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon5, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon6, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon7, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon8, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon9, 0),
                it.ref(R.styleable.ButtonGroup_ButtonGroupIcon10, 0),
            ).filter { resId -> resId != 0 }

            for (index in iconsArray.indices) {
                val tv = TextView(context)
                tv.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    if (itemGravity == AVERAGE) weight = 1f
                    else {
                        if (orientation == HORIZONTAL) setMargins(gap, 0, gap, 0)
                        else setMargins(0, gap, 0, gap)
                    }
                }
                tv.gravity = Gravity.CENTER
                val drawable = context.ri(iconsArray[index])
                if (textPosition == TOP) tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, drawable)
                else tv.setCompoundDrawablesRelativeWithIntrinsicBounds(null, drawable, null, null)
                val text = splitStrings.getOrNull(index)?.trim() ?: ""
                if (text.isEmpty()) tv.textSize = 0f
                else {
                    tv.compoundDrawablePadding = itemGap
                    tv.paint.isFakeBoldText = textBold
                    tv.textSizePx = textSize
                    tv.textColor = textColor
                    tv.text = text
                }
                tv.rachelClick { listener?.invoke(index) }
                items += tv
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        for (item in items) addView(item)
    }

    fun setItemVisibility(index: Int, visible: Boolean) {
        items[index].visible = visible
    }

    fun setItemText(index: Int, text: String) {
        items[index].text = text
    }

    fun setItemImage(index: Int, @DrawableRes res: Int) {
        items[index].apply {
            val drawable = context.ri(res)
            if (compoundDrawablesRelative[1] != null) setCompoundDrawablesRelativeWithIntrinsicBounds(null, drawable, null, null)
            else setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, drawable)
        }
    }

    fun setItemImageTint(index: Int, @ColorInt color: Int) {
        items[index].apply {
            compoundDrawablesRelative[1]?.setTint(color)
            compoundDrawablesRelative[3]?.setTint(color)
        }
    }

    fun setItemTag(index: Int, tag: Any?) {
        items[index].tag = tag
    }

    fun getItemTag(index: Int): Any? = items[index].tag
}