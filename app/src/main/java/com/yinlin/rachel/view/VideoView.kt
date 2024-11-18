package com.yinlin.rachel.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.shuyu.gsyvideoplayer.cache.CacheFactory
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.yinlin.rachel.visible


class VideoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : StandardGSYVideoPlayer(context, attrs) {

    companion object {
        init {
            PlayerFactory.setPlayManager(IjkPlayerManager::class.java)
            CacheFactory.setCacheManager(ProxyCacheManager::class.java)
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
            GSYVideoType.setRenderType(GSYVideoType.GLSURFACE)
            GSYVideoType.enableMediaCodec()
            GSYVideoType.enableMediaCodecTexture()
            GSYVideoType.isMediaCodec()
            GSYVideoType.isMediaCodecTexture()
        }
    }

    init {
        mOrientationUtils = OrientationUtils(context as Activity, this)
        mOrientationUtils.isEnable = true
        mOrientationUtils.isRotateWithSystem = false
        isNeedOrientationUtils = true
        backButton.visible = false
        titleTextView.visible = false
        fullscreenButton.visible = false
        isShowFullAnimation = true
        isRotateWithSystem = false
        isAutoFullWithSize = false
        isRotateViewAuto = false
        isNeedShowWifiTip = false
        isNeedLockFull = true
        isLockLand = false
        isLooping = true
        setIsTouchWigetFull(true)
        setVideoAllCallBack(object : GSYSampleCallBack() {
            override fun onPrepared(url: String?, vararg objects: Any?) {
                super.onPrepared(url, *objects)
                if (!isVerticalVideo) mOrientationUtils.resolveByClick()
                mOrientationUtils.isEnable = false
            }
            override fun onPlayError(url: String?, vararg objects: Any?) {
                super.onPlayError(url, *objects)
                println(objects[2] as Int)
            }
        })
    }

    override fun isLockLandByAutoFullSize() = !isVerticalVideo

    fun play(uri: String?) {
        uri?.let {
            setUp(it, true, "")
            startPlayLogic()
        }
    }
}