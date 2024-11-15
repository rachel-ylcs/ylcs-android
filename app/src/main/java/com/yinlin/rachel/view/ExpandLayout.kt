package com.yinlin.rachel.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr


class ExpandLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    enum class State {
        COLLAPSED, COLLAPSING, EXPANDING, EXPANDED
    }

    companion object {
        const val HORIZONTAL = 0
        const val VERTICAL = 1
    }

    private val mDuration: Long
    private val mParallax: Float
    private var mExpansion: Float
    private val mOrientation: Int
    private var mState: State
    private val mInterpolator = FastOutSlowInInterpolator()
    private var mAnimator: ValueAnimator? = null

    init {
        RachelAttr(context, attrs, R.styleable.ExpandLayout).use {
            mDuration = it.value(R.styleable.ExpandLayout_Duration, 300).toLong()
            mExpansion = if (it.value(R.styleable.ExpandLayout_Expanded, false)) 1f else 0f
            mParallax = 1f.coerceAtMost(0f.coerceAtLeast(it.value(R.styleable.ExpandLayout_Parallax, 1f)))
            mOrientation = it.value(R.styleable.ExpandLayout_android_orientation, VERTICAL)
        }

        mState = if (mExpansion == 0f) State.COLLAPSED else State.EXPANDED
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth
        val h = measuredHeight
        val size = if (mOrientation == LinearLayout.HORIZONTAL) w else h
        visibility = if (mExpansion == 0f && size == 0) GONE else VISIBLE
        val delta = size - Math.round(size * mExpansion)
        if (mParallax > 0) {
            val parallaxDelta = delta * mParallax
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (mOrientation == HORIZONTAL) child.translationX = -parallaxDelta
                else child.translationY = -parallaxDelta
            }
        }
        if (mOrientation == HORIZONTAL) setMeasuredDimension(w - delta, h)
        else setMeasuredDimension(w, h - delta)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        mAnimator?.cancel()
        super.onConfigurationChanged(newConfig)
    }

    private fun setExpansion(expansion: Float) {
        if (mExpansion == expansion) return
        val delta = expansion - mExpansion
        mState = if (expansion == 0f) State.COLLAPSED
        else if (expansion == 1f) State.EXPANDED
        else if (delta < 0) State.COLLAPSING
        else State.EXPANDING
        visibility = if (mState == State.COLLAPSED) GONE else VISIBLE
        mExpansion = expansion
        requestLayout()
    }

    private fun animateSize(targetExpansion: Float) {
        mAnimator?.cancel()
        mAnimator = ValueAnimator.ofFloat(mExpansion, targetExpansion)
        mAnimator?.apply {
            interpolator = mInterpolator
            duration = mDuration
            addUpdateListener { setExpansion(it.animatedValue as Float) }
            addListener(object : Animator.AnimatorListener {
                private var canceled = false
                override fun onAnimationStart(animation: Animator) {
                    mState = if (targetExpansion == 0f) State.COLLAPSING else State.EXPANDING
                }
                override fun onAnimationEnd(animation: Animator) {
                    if (!canceled) {
                        mState = if (targetExpansion == 0f) State.COLLAPSED else State.EXPANDED
                        setExpansion(targetExpansion)
                    }
                }
                override fun onAnimationCancel(animation: Animator) { canceled = true }
                override fun onAnimationRepeat(animation: Animator) { }
            })
            start()
        }
    }

    fun update(expand: Boolean, animate: Boolean) {
        if (expand == isExpand) return
        val targetExpansion = if (expand) 1f else 0f
        if (animate) animateSize(targetExpansion)
        else setExpansion(targetExpansion)
    }

    fun expand(animate: Boolean) = update(true, animate)
    fun collapse(animate: Boolean) = update(false, animate)
    fun toggle(animate: Boolean = true) {
        if (isExpand) collapse(animate)
        else expand(animate)
    }

    var isExpand: Boolean
        get() = mState == State.EXPANDING || mState == State.EXPANDED
        set(value) { update(value, true) }
}