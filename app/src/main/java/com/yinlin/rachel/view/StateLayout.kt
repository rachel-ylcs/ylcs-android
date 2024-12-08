package com.yinlin.rachel.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelAttr
import com.yinlin.rachel.tool.ra
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.visible

class StateLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr) {
    enum class State {
        Empty, Error, Offline, Loading, Content
    }
    private var mState: State = State.Content
    private val animationIn: Animation
    private val animationOut: Animation
    private val errorText: String
    private val offlineText: String
    private val emptyText: String
    private val loadingText: String
    private val retryText: String

    private lateinit var mContainer: LinearLayout
    private lateinit var mProgress: ProgressBar
    private lateinit var mImage: ImageView
    private lateinit var mText: TextView
    private lateinit var mButton: Button
    private lateinit var mContent: View

    private var counter: Int = 0

    init {
        RachelAttr(context, attrs, R.styleable.StateLayout).use {
            animationIn = context.ra(it.ref(R.styleable.StateLayout_StateLayout_AnimationIn, R.anim.anim_fade_in))
            animationOut = context.ra(it.ref(R.styleable.StateLayout_StateLayout_AnimationOut, R.anim.anim_fade_out))
            errorText = it.value(R.styleable.StateLayout_StateLayout_ErrorText, "发生错误, 请重试")
            offlineText = it.value(R.styleable.StateLayout_StateLayout_OfflineText, "世界上最遥远的距离是没有网络")
            emptyText = it.value(R.styleable.StateLayout_StateLayout_EmptyText, "感觉空空的....")
            loadingText = it.value(R.styleable.StateLayout_StateLayout_LoadingText, "加载中....")
            retryText = it.value(R.styleable.StateLayout_StateLayout_RetryText, "重试")
        }
        orientation = VERTICAL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount != 1) throw IllegalStateException("childViewCount != 1")
        mContent = getChildAt(0)
        if (isInEditMode) return
        mContainer = LayoutInflater.from(context).inflate(R.layout.template_state_layout, this, false) as LinearLayout
        addView(mContainer)
        mProgress = mContainer.findViewById(R.id.progress)
        mImage = mContainer.findViewById(R.id.image)
        mText = mContainer.findViewById(R.id.text)
        mButton = mContainer.findViewById(R.id.button)
        mButton.text = retryText
    }

    private fun changeState(callback: (() -> Unit)?) {
        when (mState) {
            State.Empty -> {
                mText.text = emptyText
                mProgress.visible = false
                mImage.visible = true
                mImage.setImageResource(R.drawable.icon_empty)
                mButton.visible = false
            }
            State.Error -> {
                mText.text = errorText
                mProgress.visible = false
                mImage.visible = true
                mImage.setImageResource(R.drawable.icon_error)
                if (callback != null) {
                    mButton.visible = true
                    mButton.rachelClick { callback() }
                }
                else mButton.visible = false
            }
            State.Offline -> {
                mText.text = offlineText
                mProgress.visible = false
                mImage.visible = true
                mImage.setImageResource(R.drawable.icon_offline)
                if (callback != null) {
                    mButton.visible = true
                    mButton.rachelClick { callback() }
                }
                else mButton.visible = false
            }
            State.Loading -> {
                mText.text = loadingText
                mProgress.visible = true
                mImage.visible = false
                mButton.visible = false
            }
            State.Content -> { }
        }
    }

    private fun showState(nextState: State, callback: (() -> Unit)? = null) {
        mState = nextState
        mContainer.clearAnimation()
        mContent.clearAnimation()
        val cnt = ++counter
        if (mContainer.visible) {
            animationOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { }
                override fun onAnimationEnd(animation: Animation?) {
                    if (cnt == counter) {
                        changeState(callback)
                        mContainer.startAnimation(animationIn)
                    }
                }
            })
            mContainer.startAnimation(animationOut)
        }
        else {
            animationOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { }
                override fun onAnimationEnd(animation: Animation?) {
                    if (cnt == counter) {
                        mContent.visible = false
                        mContainer.visible = true
                        mContainer.startAnimation(animationIn)
                    }
                }
            })
            mContent.startAnimation(animationOut)
            changeState(callback)
        }
    }

    val state: State get() = mState
    val isEmpty: Boolean get() = state == State.Empty
    val isError: Boolean get() = state == State.Error
    val isOffline: Boolean get() = state == State.Offline
    val isLoading: Boolean get() = state == State.Loading
    val isContent: Boolean get() = state == State.Content

    fun showContent() {
        mState = State.Content
        mContainer.clearAnimation()
        mContent.clearAnimation()
        val cnt = ++counter
        if (mContainer.visible) {
            animationOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { }
                override fun onAnimationEnd(animation: Animation?) {
                    if (cnt == counter) {
                        mContainer.visible = false
                        mContent.visible = true
                        mContent.startAnimation(animationIn)
                    }
                }
            })
            mContainer.startAnimation(animationOut)
        }
    }

    fun showEmpty() = showState(State.Empty)
    fun showError(callback: (() -> Unit)? = null) = showState(State.Error, callback)
    fun showOffline(callback: (() -> Unit)? = null) = showState(State.Offline, callback)
    fun showLoading() = showState(State.Loading)
}