package com.yinlin.rachel.model

import android.os.CountDownTimer

class RachelTimer {
    fun interface Listener {
        fun onStart() { }
        fun onStop() { }
        fun onTick(remain: Long) { }
        fun onFinish()
    }

    private var mTimer: CountDownTimer? = null
    private val mListeners = mutableListOf<Listener>()
    private val mLock = Any()
    val isStart: Boolean get() = mTimer != null

    fun start(duration: Long, tick: Long, listener: Listener? = null) {
        mTimer?.cancel()
        listener?.let { mListeners.add(it) }
        mTimer = object : CountDownTimer(duration, tick) {
            override fun onTick(millisUntilFinished: Long) {
                synchronized(mLock) {
                    for (item in mListeners) item.onTick(millisUntilFinished)
                }
            }
            override fun onFinish() { stop(true) }
        }.let {
            synchronized(mLock) {
                for (item in mListeners) item.onStart()
            }
            it.start()
        }
    }

    fun cancel() {
        mTimer?.cancel()
        stop(false)
    }

    fun addListener(listener: Listener) {
        synchronized(mLock) { mListeners.add(listener) }
    }

    fun removeListener(listener: Listener) {
        synchronized(mLock) { mListeners.remove(listener) }
    }

    private fun stop(isFinish: Boolean) {
        synchronized(mLock) {
            for (item in mListeners) item.onStop()
            if (isFinish) {
                for (item in mListeners) item.onFinish()
            }
            mListeners.clear()
        }
        mTimer = null
    }
}