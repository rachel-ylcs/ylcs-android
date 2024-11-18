package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelFrequencyCounter
import com.yinlin.rachel.timeString
import com.yinlin.rachel.toDP
import com.yinlin.rachel.toSP
import kotlin.math.abs


class HotpotProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    fun interface OnProgressChangedListener {
        fun onProgressChanged(percent: Float)
    }

    private var played: Long = 0L
    private var duration: Long = 0L
    private var items: List<Long> = ArrayList()
    private val paintTotal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.music_progress_total)
    }
    private val paintPlayed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.music_progress_played)
    }
    private val paintHotpot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.music_progress_hotpot)
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.white)
        textSize = 12f.toSP(context)
        typeface = context.resources.getFont(R.font.xwwk)
    }
    private val progressHeight = 10.toDP(context)
    private val gap = 5.toDP(context)
    private val textHeight = paintText.getFontMetrics().let { it.descent - it.ascent }
    private val updateCounter: RachelFrequencyCounter = RachelFrequencyCounter(4)
    private var listener: OnProgressChangedListener? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (progressHeight + textHeight + gap).toInt())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()
        val playedWidth = if (duration == 0L) 0f else played * canvasWidth / duration
        val progressTop = textHeight + gap
        val playedText = played.timeString
        val totalText = duration.timeString
        val centerProgressY = progressTop + (canvasHeight - progressTop) / 2
        val progressHeightHalf = (canvasHeight - progressTop) / 6
        val radius = progressHeightHalf * 2
        canvas.apply {
            drawText(playedText, 0f, textHeight, paintText)
            drawText(totalText, canvasWidth - paintText.measureText(totalText), textHeight, paintText)
            drawRoundRect(0f, centerProgressY + progressHeightHalf, canvasWidth, centerProgressY - progressHeightHalf, radius, radius, paintTotal)
            drawRoundRect(0f, centerProgressY + progressHeightHalf, playedWidth, centerProgressY - progressHeightHalf, radius, radius, paintPlayed)
            if (duration != 0L) {
                for (position in items) drawCircle(position * canvasWidth / duration, centerProgressY, radius, paintHotpot)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && duration != 0L) {
            val canvasWidth = width
            val x = event.x
            if (event.y > textHeight + gap) {
                val radiusX = height / 2f
                var percent = x / canvasWidth
                if (items.isNotEmpty()) { // 快进副歌事件
                    for (position in items) { // 搜索副歌热点事件
                        val hotpotX = position.toFloat() * canvasWidth / duration
                        if (abs((x - hotpotX).toDouble()) <= radiusX) { // 如果误差不超过副歌条的阈值
                            percent = position.toFloat() / duration
                            break
                        }
                    }
                }
                listener?.onProgressChanged(percent) // 普通进度变更事件
            }
        }
        return true
    }

    fun setInfo(items: List<Long>, duration: Long) {
        this.items = items
        this.duration = duration
        this.played = 0L
        updateCounter.reset()
        invalidate()
    }

    fun updateProgress(position: Long, immediately: Boolean) {
        if (immediately) {
            this.played = position
            invalidate()
        } else if (updateCounter.ok()) {
            this.played = position
            invalidate()
        }
    }

    fun setOnProgressChangedListener(listener: OnProgressChangedListener) {
        this.listener = listener
    }
}