package com.yinlin.rachel.activity

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager
import com.yinlin.rachel.R
import com.yinlin.rachel.model.RachelActivity
import com.yinlin.rachel.view.VideoView

class VideoActivity : RachelActivity() {
    private lateinit var player: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) window.insetsController?.hide(WindowInsets.Type.statusBars())
        else window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_video)

        player = findViewById(R.id.player)
        player.play(intent.getStringExtra("uri"))
    }

    override fun onPause() {
        super.onPause()
        player.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        player.onVideoResume()
    }

    override fun onDestroy() {
        GSYVideoManager.releaseAllVideos()
        player.setVideoAllCallBack(null)
        player.release()
        ProxyCacheManager.getProxy(this, null).shutdown()
        super.onDestroy()
    }
}