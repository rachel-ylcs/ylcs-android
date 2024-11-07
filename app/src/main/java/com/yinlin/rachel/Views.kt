package com.yinlin.rachel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.haibin.calendarview.Calendar
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

@SuppressLint("InflateParams")
private fun View.tip(text: String, @ColorRes color: Int) {
    val bar = Snackbar.make(this, "", 1500)
    val layout = bar.view as ViewGroup
    layout.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).visible = false
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_tip, null)
    view.findViewById<TextView>(R.id.text).text = text
    view.findViewById<MaterialCardView>(R.id.card).setCardBackgroundColor(context.getColor(color))
    layout.setPadding(0, 0, 0, 0)
    layout.backgroundColor = 0
    layout.addView(view)
    bar.show()
}

fun View.tipSuccess(text: String) = tip(text, R.color.tip_success)
fun View.tipWarning(text: String) = tip(text, R.color.tip_warning)
fun View.tipError(text: String) = tip(text, R.color.tip_error)
fun View.tipInfo(text: String) = tip(text, R.color.tip_info)

/*---------    TextInputEditText    --------*/

var TextInputEditText.content: String
    get() = text?.toString() ?: ""
    set(value) { setText(value) }

/*---------    TextView    --------*/

var TextView.textColor: Int @ColorInt
get() = currentTextColor
    set(value) { setTextColor(value) }
var TextView.strikethrough: Boolean
    get() = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG == Paint.STRIKE_THRU_TEXT_FLAG
    set(value) {
        paintFlags = if (value) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

/*---------    Paint    --------*/

val Paint.textHeight: Float get() = this.fontMetrics.descent - this.fontMetrics.ascent

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