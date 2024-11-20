package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAppIntent

class StudioView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    data class Item(val x: Int, val y: Int, val w: Int, val h: Int, val id: String, val qq: Boolean = true)
    private val items = arrayOf(
        Item(686, 501, 129, 172, "828049503", false),
        Item(218, 470, 83, 154, "3358037803"),
        Item(101, 734, 88, 100, "1148361865"),
        Item(484, 549, 47, 105, "3163099920"),
        Item(25, 872, 108, 108, "3512375559"),
        Item(399, 822, 72, 90, "1458184507"),
        Item(679, 688, 88, 92, "3540899447"),
        Item(269, 1015, 78, 93, "1743694231"),
        Item(572, 915, 81, 105, "3494737428"),
        Item(714, 879, 76, 102, "1052405595"),
        Item(867, 866, 69, 111, "473647171"),
        Item(146, 1482, 78, 91, "1002454170"),
        Item(268, 1375, 100, 118, "2194686361"),
        Item(612, 1147, 91, 101, "1595810301"),
        Item(802, 1016, 72, 105, "2509645478"),
        Item(255, 1593, 86, 96, "393292044"),
        Item(397, 1503, 82, 106, "1278315079"),
        Item(581, 1298, 140, 153, "3257584574"),
        Item(736, 1215, 98, 108, "241009018"),
        Item(876, 1309, 122, 136, "2682103669"),
    )

    init {
        adjustViewBounds = true
        setImageResource(R.drawable.img_studio)
    }

//    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        style = Paint.Style.STROKE
//        strokeWidth = 10f
//        color = Color.RED
//    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        val ratio = 1080f / measuredWidth
//        for (item in items) {
//            canvas.drawRect(item.x / ratio, item.y / ratio, (item.x + item.w) / ratio, (item.y + item.h) / ratio, paint)
//        }
//    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && measuredWidth != 0) {
            val ratio = 1080f / measuredWidth
            val x = (event.x * ratio).toInt()
            val y = (event.y * ratio).toInt()
            for (item in items) {
                if (isActive(x, y, item)) {
                    val intent = if (item.qq) RachelAppIntent.QQ(item.id) else RachelAppIntent.QQGroup(item.id)
                    intent.start(context)
                    break
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun isActive(x: Int, y: Int, item: Item): Boolean =
        x > item.x && x < item.x + item.w && y > item.y && y < item.y + item.h
}