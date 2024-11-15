package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import org.libpag.PAGFile
import org.libpag.PAGScaleMode
import org.libpag.PAGView

@SuppressLint("RestrictedApi")
class PAGRefreshHeader @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : PAGView(context, attrs, defStyleAttr), RefreshHeader {
    init {
        setRepeatCount(-1)
        setScaleMode(PAGScaleMode.Zoom)
        setCacheEnabled(true)

        RachelAttr(context, attrs, R.styleable.PAGRefreshHeader).use {
            val assetName = it.value(R.styleable.PAGRefreshHeader_AssetName, "")
            if (assetName.isNotEmpty()) composition = PAGFile.Load(context.resources.assets, assetName)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (composition != null) {
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val newHeightMeasureSpec = resolveSize(widthSize * composition.height() / composition.width(), heightMeasureSpec)
            setMeasuredDimension(widthMeasureSpec, newHeightMeasureSpec)
        }
    }

    override fun onStateChanged(refreshLayout: RefreshLayout, oldState: RefreshState, newState: RefreshState) {
        when (newState) {
            RefreshState.PullDownToRefresh -> progress = 0.0
            RefreshState.Refreshing -> play()
            else -> {}
        }
    }

    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int = 500
    override fun getView(): View = this
    override fun getSpinnerStyle(): SpinnerStyle = SpinnerStyle.Translate
    override fun setPrimaryColors(vararg colors: Int) { }
    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) { }
    override fun onMoving(isExpand: Boolean, p1: Float, p2: Int, p3: Int, p4: Int) {
        if (!isExpand && p1 == 0f && p2 == 0) pause()
    }
    override fun onHorizontalDrag(p0: Float, p1: Int, p2: Int) { }
    override fun onReleased(refreshLayout: RefreshLayout, p1: Int, p2: Int) { }
    override fun onStartAnimator(refreshLayout: RefreshLayout, p1: Int, p2: Int) { }
    override fun isSupportHorizontalDrag(): Boolean = false
    override fun autoOpen(p0: Int, p1: Float, p2: Boolean): Boolean = false
}