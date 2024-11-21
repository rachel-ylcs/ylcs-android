package com.yinlin.rachel.dialog

import android.text.InputType
import com.yinlin.rachel.R
import com.yinlin.rachel.Tip
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.databinding.BottomDialogSleepModeBinding
import com.yinlin.rachel.fragment.FragmentMusic
import com.yinlin.rachel.model.RachelBottomDialog
import com.yinlin.rachel.model.RachelDialog
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.model.RachelTimer
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.timeString
import okhttp3.internal.toLongOrDefault

class BottomDialogSleepMode(fragment: FragmentMusic) : RachelBottomDialog<BottomDialogSleepModeBinding, FragmentMusic>(
    fragment, 0.6f, BottomDialogSleepModeBinding::class.java), RachelTimer.Listener {
    private var isUpdate = false

    override fun init() {
        v.pic.load(R.drawable.image_sleep)

        v.start.rachelClick {
            val timer = root.sleepModeTimer
            if (timer.isStart) RachelDialog.confirm(root.main, content="关闭睡眠模式") { timer.cancel() }
            else RachelDialog.input(root.main, content="设定睡眠模式(1 ~ 1000分钟)", maxLength = 3,
                inputType = InputType.TYPE_CLASS_NUMBER) {
                val minutes = it.toLongOrDefault(0).coerceAtLeast(0)
                if (minutes <= 0) tip(Tip.WARNING, "睡眠时间应在1 ~ 1000分钟间")
                else timer.start(minutes * 60 * 1000, 1000, this)
            }
        }
    }

    override fun hidden() {
        isUpdate = false
    }

    fun update(): BottomDialogSleepMode {
        isUpdate = true
        return this
    }

    override fun onStart() {
        v.start.load(R.drawable.icon_stop_blue)
    }

    override fun onStop() {
        v.time.text = "00:00:00"
        v.start.load(R.drawable.icon_start_blue)
    }

    override fun onTick(remain: Long) {
        if (isUpdate) v.time.text = remain.timeString
    }

    override fun onFinish() {
        root.main.sendMessage(RachelTab.music, RachelMessage.MUSIC_PAUSE_PLAYER)
    }
}