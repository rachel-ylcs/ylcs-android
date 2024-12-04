package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import com.yinlin.rachel.R
import com.yinlin.rachel.tool.baseLine
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rf
import com.yinlin.rachel.tool.textHeight

class UserLabelView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    companion object {
        private const val RATIO = 21 / 8f

        private val labelNameFromLevel = arrayOf("BUG",
            "风露婆娑", "剑心琴魄", "梦外篝火", "烈火胜情爱", "青山撞入怀",
            "雨久苔如海", "明雪澄岚", "春风韵尾", "银河万顷", "山川蝴蝶",
            "薄暮忽晚", "沧流彼岸", "清荷玉盏", "颜如舜华", "逃奔风月",
            "自在盈缺", "青鸟遁烟", "天生妙罗帷", "梦醒般惊蜕", "韶华的结尾",
        )

        @DrawableRes fun labelImageFromLevel(level: Int): Int = when (level) {
            in 1..3 -> R.drawable.label_fucaoweiying
            in 4..6 -> R.drawable.label_pifuduhai
            in 7..10 -> R.drawable.label_fenghuaxueyue
            in 11..13 -> R.drawable.label_liuli
            in 14..17 -> R.drawable.label_lidishigongfen
            in 18..99 -> R.drawable.label_shanseyouwuzhong
            else -> R.drawable.label_fucaoweiying
        }

        @DrawableRes fun labelImageFromName(name: String): Int = R.drawable.label_special
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var text: String = labelNameFromLevel[1]

    init {
        paint.typeface = context.rf(R.font.xwwk)
        paint.isFakeBoldText = true
        paint.color = context.rc(R.color.title_dark)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 4f
        setImageResource(R.drawable.label_fucaoweiying)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(widthSize, (widthSize / RATIO).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resizeText(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(text, width * 0.5f, height * 0.65f + paint.baseLine, paint)
    }

    private fun resizeText(w: Int, h: Int) {
        val maxTextWidth = w * 0.55f
        val maxTextHeight = h * 0.4f
        while (paint.textHeight < maxTextHeight) {
            if (paint.measureText(text) > maxTextWidth) break
            paint.textSize += 1
        }
    }

    fun setLabel(label: String, level: Int) {
        text = label.ifEmpty { labelNameFromLevel[level] }
        setImageResource(if (label.isEmpty()) labelImageFromLevel(level) else labelImageFromName(label))
        resizeText(measuredWidth, measuredHeight)
    }

    fun setDefaultLabel() = setLabel("", 1)
}