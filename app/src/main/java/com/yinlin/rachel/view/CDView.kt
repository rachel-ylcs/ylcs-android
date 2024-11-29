package com.yinlin.rachel.view

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.tool.pureColor
import java.io.File

class CDView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {
    private val pic = AvatarView(context)
    private val animator = AnimatorInflater.loadAnimator(context, R.animator.animator_cd) as ObjectAnimator
    private var angle: Float = 0f

    init {
        setBackgroundResource(R.drawable.img_music_record)

        pic.layoutParams = LayoutParams(0, 0).apply {
            matchConstraintPercentWidth = 0.75f
            dimensionRatio = "1:1"
            leftToLeft = 0
            rightToRight = 0
            topToTop = 0
            bottomToBottom = 0
        }
        addView(pic)

        animator.apply {
            target = pic
            addUpdateListener { angle = animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = setFloatValues(angle, angle + 360f)
            })
        }
    }

    fun loadCD(file: File) { pic.load(file) }
    fun clearCD() { pic.pureColor = 0 }
    fun startCD() = if (animator.isPaused) animator.resume() else animator.start()
    fun pauseCD() = animator.pause()
}