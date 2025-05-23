package com.yinlin.rachel.sheet

import com.yinlin.rachel.R
import com.yinlin.rachel.annotation.SheetLayout
import com.yinlin.rachel.databinding.SheetSleepModeBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.model.RachelTimer
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.timeStringWithHour

@SheetLayout(SheetSleepModeBinding::class, 0.6f)
class SheetSleepMode(fragment: FragmentMusic, private val timer: RachelTimer)
    : RachelSheet<SheetSleepModeBinding, FragmentMusic>(fragment) {
    private val listener = object : RachelTimer.Listener {
        override fun onTick(remain: Long) { v.time.text = remain.timeStringWithHour }
        override fun onStop() { dismiss() }
        override fun onFinish() { }
    }

    override fun init() {
        timer.addListener(listener)
        v.pic.load(R.drawable.image_sleep)
        v.stop.rachelClick {
            if (timer.isStart) RachelDialog.confirm(fragment.main, content="关闭睡眠模式") { timer.cancel() }
        }
    }

    override fun quit() {
        timer.removeListener(listener)
    }
}