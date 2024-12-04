package com.yinlin.rachel.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import com.haibin.calendarview.MonthView
import com.yinlin.rachel.R
import com.yinlin.rachel.data.activity.ShowActivityPreviewList
import com.yinlin.rachel.tool.rc
import com.yinlin.rachel.tool.rf
import com.yinlin.rachel.tool.toDP

class ActivityCalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : CalendarView(context, attrs) {
    class RachelMonthView(context: Context) : MonthView(context) {
        private val colorNormal = context.rc(R.color.calendar_normal)
        private val colorOther = context.rc(R.color.calendar_other)
        private val colorNow = context.rc(R.color.calendar_now)
        private val colorWeekend = context.rc(R.color.calendar_weekend)
        private val colorActivity = context.rc(R.color.calendar_activity)
        private var showStar = run {
            val drawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.icon_star_colorful)!!).mutate()
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }

        private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = context.resources.getDimension(R.dimen.base)
            textAlign = Paint.Align.CENTER
            typeface = context.rf(R.font.xwwk)
            color = colorNormal
            isFakeBoldText = true
        }
        private val lunarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = context.resources.getDimension(R.dimen.xxs)
            textAlign = Paint.Align.CENTER
            typeface = context.rf(R.font.xwwk)
            color = colorNormal
        }
        private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = colorActivity
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f.toDP(context)
            color = colorNow
        }

        override fun onDrawSelected(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean) = true

        override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) {
            val pad = 1f.toDP(context)
            canvas.drawCircle(x + mItemWidth / 2f, y + mItemHeight - 6 * pad, 2 * pad, pointPaint)
            canvas.drawBitmap(showStar, x + mItemWidth - showStar.width - pad, y + pad, null)
        }

        override fun onDrawText(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean, isSelected: Boolean) {
            val cx = x + mItemWidth / 2
            val top = y - mItemHeight / 6
            dayPaint.color = if (calendar.hasScheme()) colorActivity
            else if (calendar.isCurrentDay) colorNow
            else if (!calendar.isCurrentMonth) colorOther
            else if (calendar.isWeekend) colorWeekend
            else colorNormal
            lunarPaint.color = if (calendar.hasScheme()) colorActivity
            else if (calendar.isCurrentDay) colorNow
            else if (!calendar.isCurrentMonth) colorOther
            else if (calendar.isWeekend) colorWeekend
            else colorNormal
            val dayText = if (calendar.hasScheme()) calendar.scheme else calendar.lunar
            lunarPaint.textSize = context.resources.getDimension(
                if (lunarPaint.measureText(dayText) >= mItemWidth) R.dimen.xxxs else R.dimen.xxs)

            canvas.drawText(calendar.day.toString(), cx.toFloat(), mTextBaseLine + top, dayPaint)
            canvas.drawText(dayText, cx.toFloat(), mTextBaseLine + y + mItemHeight / 10f, lunarPaint)

            val linePad = 7f.toDP(context)
            if (!calendar.hasScheme() && calendar.isCurrentDay) canvas.drawLine(x + linePad, y + mItemHeight - linePad,
                x + mItemWidth - linePad, y + mItemHeight - linePad, linePaint)
        }
    }

    interface Listener {
        fun onClick(calendar: Calendar)
        fun onLongClick(calendar: Calendar)
        fun onMonthChanged(year: Int, month: Int)
    }

    var listener: Listener? = null

    init {
        setMonthView(RachelMonthView::class.java)
        setAllMode()
        setWeekStarWithMon()
        setSelectSingleMode()
        setCalendarItemHeight(50.toDP(context))
        var minYear = curYear
        var maxYear = curYear
        var minMonth = curMonth - 5
        var maxMonth = curMonth + 5
        if (curMonth < 6) {
            --minYear
            minMonth += 12
        }
        else if (curMonth > 7) {
            ++maxYear
            maxMonth -= 12
        }
        setRange(minYear, minMonth, 1, maxYear, maxMonth, -1)
        setOnCalendarSelectListener(object : OnCalendarSelectListener {
            override fun onCalendarOutOfRange(calendar: Calendar) { }
            override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                if (isClick) listener?.onClick(calendar)
            }
        })
        setOnCalendarLongClickListener(object : OnCalendarLongClickListener {
            override fun onCalendarLongClickOutOfRange(calendar: Calendar) { }
            override fun onCalendarLongClick(calendar: Calendar) { listener?.onLongClick(calendar) }
        }, true)
        setOnMonthChangeListener { year, month -> listener?.onMonthChanged(year, month) }
    }

    fun setActivities(activities: ShowActivityPreviewList) = setSchemeDate(
        activities.filter { it.calendar != null }.associate {
            val calendar = it.calendar
            calendar.toString() to calendar
        })

    fun addActivity(calendar: Calendar, title: String) {
        addSchemeDate(Calendar().apply {
            year = calendar.year
            month = calendar.month
            day = calendar.day
            scheme = title
        })
    }
}