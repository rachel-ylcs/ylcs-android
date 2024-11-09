package com.yinlin.rachel.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import com.yinlin.rachel.R
import kotlin.math.min

class AvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    private val mDrawableRect = RectF()
    private val mBorderRect = RectF()
    private val mShaderMatrix = Matrix()
    private val mBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCircleBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mBorderColor: Int
    private val mBorderWidth: Int
    private val mCircleBackgroundColor: Int
    private var mBitmapWidth = 0
    private var mBitmapHeight = 0
    private var mDrawableRadius = 0f
    private var mBorderRadius = 0f
    private var mBitmap: Bitmap? = null
    private var mColorFilter: ColorFilter? = null

    init {
        val attr = context.obtainStyledAttributes(attrs, R.styleable.AvatarView)
        mBorderWidth = attr.getDimensionPixelSize(R.styleable.AvatarView_borderWidth, 0)
        mBorderColor = attr.getColor(R.styleable.AvatarView_borderColor, context.getColor(R.color.light_gray))
        mCircleBackgroundColor = attr.getColor(R.styleable.AvatarView_bgColor, context.getColor(R.color.white))
        attr.recycle()

        scaleType = ScaleType.CENTER_CROP
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val bounds = Rect()
                mBorderRect.roundOut(bounds)
                outline.setRoundRect(bounds, bounds.width() / 2.0f)
            }
        }
    }

    private fun setup() {
        if (width != 0 || height != 0) {
            mBitmap?.let {
                val shader = BitmapShader(it, TileMode.CLAMP, TileMode.CLAMP)
                mBitmapPaint.isAntiAlias = true
                mBitmapPaint.isDither = true
                mBitmapPaint.isFilterBitmap = true
                mBitmapPaint.setShader(shader)
                mBorderPaint.style = Paint.Style.STROKE
                mBorderPaint.isAntiAlias = true
                mBorderPaint.color = mBorderColor
                mBorderPaint.strokeWidth = mBorderWidth.toFloat()
                mCircleBackgroundPaint.style = Paint.Style.FILL
                mCircleBackgroundPaint.isAntiAlias = true
                mCircleBackgroundPaint.color = mCircleBackgroundColor
                mBitmapHeight = it.height
                mBitmapWidth = it.width

                val availableWidth = width - paddingLeft - paddingRight
                val availableHeight = height - paddingTop - paddingBottom
                val sideLength = min(availableWidth, availableHeight)
                val sideLeft = paddingLeft + (availableWidth - sideLength) / 2.0f
                val sideTop = paddingTop + (availableHeight - sideLength) / 2.0f
                mBorderRect.set(RectF(sideLeft, sideTop, sideLeft + sideLength, sideTop + sideLength))

                mBorderRadius = min((mBorderRect.height() - mBorderWidth) / 2.0f, (mBorderRect.width() - mBorderWidth) / 2.0f)
                mDrawableRect.set(mBorderRect)
                if (mBorderWidth > 0) mDrawableRect.inset(mBorderWidth - 1.0f, mBorderWidth - 1.0f)
                mDrawableRadius = min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f)
                mBitmapPaint.setColorFilter(mColorFilter)

                var dx = 0.0f
                var dy = 0.0f
                mShaderMatrix.set(null)
                val scale: Float
                if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
                    scale = mDrawableRect.height() / mBitmapHeight
                    dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f
                } else {
                    scale = mDrawableRect.width() / mBitmapWidth
                    dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f
                }
                mShaderMatrix.setScale(scale, scale)
                mShaderMatrix.postTranslate(dx + 0.5f + mDrawableRect.left, dy + 0.5f + mDrawableRect.top)
                shader.setLocalMatrix(mShaderMatrix)
            }
            invalidate()
        }
    }

    private fun initializeBitmap() {
        val d = drawable
        mBitmap = when (d) {
            null -> null
            is BitmapDrawable -> d.bitmap
            else -> {
                try {
                    val bitmap = if (d is ColorDrawable) Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
                    else Bitmap.createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    d.setBounds(0, 0, canvas.width, canvas.height)
                    d.draw(canvas)
                    bitmap
                } catch (ignored: Exception) { null }
            }
        }
        setup()
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        initializeBitmap()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        if (cf != mColorFilter) {
            mColorFilter = cf
            mBitmapPaint.setColorFilter(mColorFilter)
            invalidate()
        }
    }

    override fun getColorFilter() = mColorFilter

    override fun onDraw(canvas: Canvas) {
        if (mBitmap != null) {
            if (mCircleBackgroundColor != 0) canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mCircleBackgroundPaint)
            canvas.drawCircle(mDrawableRect.centerX(), mDrawableRect.centerY(), mDrawableRadius, mBitmapPaint)
            if (mBorderWidth > 0) canvas.drawCircle(mBorderRect.centerX(), mBorderRect.centerY(), mBorderRadius, mBorderPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setup()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setup()
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        setup()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - mBorderRect.centerX()
        val dy = event.y - mBorderRect.centerY()
        return (if (mBorderRect.isEmpty) true else dx * dx + dy * dy <= mBorderRadius * mBorderRadius) && super.onTouchEvent(event)
    }
}