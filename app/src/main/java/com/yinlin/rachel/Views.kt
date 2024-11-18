package com.yinlin.rachel

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.haibin.calendarview.Calendar
import com.yinlin.rachel.Tip.*
import com.yinlin.rachel.model.RachelImageLoader
import com.yinlin.rachel.model.RachelOnClickListener
import java.io.File


/*---------    Pixel Transform    --------*/

fun Float.toDP(context: Context) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
fun Float.toSP(context: Context) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics)
fun Int.toDP(context: Context) = toFloat().toDP(context).toInt()
fun Int.toSP(context: Context) = toFloat().toSP(context).toInt()

/*---------    View    --------*/

var View.backgroundColor: Int
    get() = background?.let { (it as? ColorDrawable)?.color ?: Color.TRANSPARENT } ?: Color.TRANSPARENT
    set(value) { setBackgroundColor(value) }

var View.visible: Boolean
    get() = this.visibility == View.VISIBLE
    set(value) { this.visibility = if (value) View.VISIBLE else View.GONE }

fun View.rachelClick(listener: View.OnClickListener) = setOnClickListener(RachelOnClickListener(listener))
fun View.rachelClick(delay: Long, listener: View.OnClickListener) = setOnClickListener(RachelOnClickListener(delay, listener))

@SuppressLint("ClickableViewAccessibility")
fun View.interceptScroll() {
    this.setOnTouchListener { view, _ ->
        view.parent.requestDisallowInterceptTouchEvent(true)
        false
    }
}

/*---------    Tip    --------*/

enum class Tip {
    INFO, SUCCESS, WARNING, ERROR
}

@SuppressLint("InflateParams")
fun Activity.tip(type: Tip, text: String, anchorView: View? = null) {
    val color = when (type) {
        INFO -> R.color.tip_info
        SUCCESS -> R.color.tip_success
        WARNING -> R.color.tip_warning
        ERROR -> R.color.tip_error
    }
    val bar = Snackbar.make(anchorView ?: this.window.decorView, "", 1500)
    val layout = bar.view as ViewGroup
    layout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).visible = false
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_tip, null)
    view.findViewById<TextView>(R.id.text).text = text
    view.findViewById<MaterialCardView>(R.id.card).setCardBackgroundColor(this.getColor(color))
    layout.setPadding(0, 0, 0, 0)
    layout.backgroundColor = 0
    layout.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
        setMargins(20.toDP(this@tip), 0, 20.toDP(this@tip), 100.toDP(this@tip))
        gravity = Gravity.BOTTOM
    }
    layout.addView(view)
    bar.show()
}

/*---------    TextView    --------*/

var TextView.textSizePx: Float
    get() = textSize
    set(value) { setTextSize(TypedValue.COMPLEX_UNIT_PX, value) }

var TextView.textColor: Int @ColorInt
    get() = currentTextColor
    set(value) { setTextColor(value) }

var TextView.selectorColor: Int @ColorRes
    get() = 0
    set(value) { setTextColor(context.getColorStateList(value)) }

var TextView.strikethrough: Boolean
    get() = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG == Paint.STRIKE_THRU_TEXT_FLAG
    set(value) {
        paintFlags = if (value) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

/*---------    Paint    --------*/

val Paint.textHeight: Float get() = this.fontMetrics.descent - this.fontMetrics.ascent
val Paint.baseLine: Float get() = -(this.fontMetrics.descent + this.fontMetrics.ascent) / 2f

/*---------    ImageView    --------*/

fun ImageView.load(loader: RachelImageLoader, @RawRes @DrawableRes resourceId: Int) {
    loader.glide.load(resourceId).apply(loader.options).into(this)
}

fun ImageView.load(loader: RachelImageLoader, path: String) {
    loader.glide.load(path).apply(loader.options).into(this)
}

fun ImageView.load(loader: RachelImageLoader, path: String, sign: Any) {
    loader.glide.load(path).signature(ObjectKey(sign)).apply(loader.options).into(this)
}

fun ImageView.load(loader: RachelImageLoader, file: File) {
    loader.glide.load(file).apply(loader.options).into(this)
}

fun ImageView.load(loader: RachelImageLoader, file: File, sign: Any) {
    loader.glide.load(file).signature(ObjectKey(sign)).apply(loader.options).into(this)
}

fun ImageView.clear(loader: RachelImageLoader) = loader.glide.clear(this)

var ImageView.pureColor: Int
    get() = (drawable as? ColorDrawable)?.color ?: Color.TRANSPARENT
    set(value) { setImageDrawable(if (value == Color.TRANSPARENT) null else ColorDrawable(value)) }

/*---------    Calendar    --------*/

val Calendar.date: String get() = "${this.year}-${this.month}-${this.day}"