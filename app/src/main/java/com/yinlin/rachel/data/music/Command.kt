package com.yinlin.rachel.data.music

import android.os.Bundle
import androidx.media3.session.SessionCommand

object Command {
    const val ARG_MODE = "Mode"
    const val ARG_INDEX = "Index"
    const val ARG_PROGRESS_PERCENT = "ProgressPercent"

    val CommandPlayOrPause = SessionCommand("PlayOrPause", Bundle.EMPTY)
    val CommandPause = SessionCommand("Pause", Bundle.EMPTY)
    val CommandStop = SessionCommand("Stop", Bundle.EMPTY)
    val CommandGotoPrevious = SessionCommand("GotoPrevious", Bundle.EMPTY)
    val CommandGotoNext = SessionCommand("GotoNext", Bundle.EMPTY)
    val CommandGotoIndex = SessionCommand("GotoIndex", Bundle.EMPTY)
    val CommandGetMode = SessionCommand("GetMode", Bundle.EMPTY)
    val CommandNextMode = SessionCommand("NextMode", Bundle.EMPTY)
    val CommandSetProgressPercent = SessionCommand("SetProgressPercent", Bundle.EMPTY)
    val CommandShuffle = SessionCommand("Shuffle", Bundle.EMPTY)
}