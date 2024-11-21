package com.yinlin.rachel.model

import android.os.CountDownTimer

class RachelTimer {
    interface Listener {
        fun onStart()
        fun onStop()
        fun onTick(remain: Long)
        fun onFinish()
    }

    private var mTimer: CountDownTimer? = null
    private var mListener: Listener? = null

    val isStart: Boolean get() = mTimer != null

    fun start(duration: Long, tick: Long, listener: Listener) {
        mTimer?.cancel()
        mListener = listener
        mTimer = object : CountDownTimer(duration, tick) {
            override fun onTick(millisUntilFinished: Long) {
                mListener?.onTick(millisUntilFinished)
            }
            override fun onFinish() {
                mListener?.onStop()
                mListener?.onFinish()
                mTimer = null
                mListener = null
            }
        }.let {
            mListener?.onStart()
            it.start()
        }
    }

    fun cancel() {
        mTimer?.cancel()
        mListener?.onStop()
        mTimer = null
        mListener = null
    }
}