package com.yinlin.rachel.sheet

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.yinlin.rachel.Config
import com.yinlin.rachel.R
import com.yinlin.rachel.attachAlpha
import com.yinlin.rachel.backgroundColor
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.databinding.SheetLyricsSettingsBinding
import com.yinlin.rachel.detachAlpha
import com.yinlin.rachel.fragment.FragmentSettings
import com.yinlin.rachel.getAlpha
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelSheet
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.rachelClick
import com.yinlin.rachel.textColor

class SheetLyricsSettings(fragment: FragmentSettings) : RachelSheet<SheetLyricsSettingsBinding, FragmentSettings>(fragment, 0.8f) {
    private val canShow: Boolean get() = Settings.canDrawOverlays(context)

    private val lyricsSettings = Config.music_lyrics_settings

    override fun bindingClass() = SheetLyricsSettingsBinding::class.java

    override fun init() {
        updateFloatingIconStatus()
        v.floating.rachelClick {
            updateFloatingIconStatus()
            if (!canShow) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.setData(Uri.fromParts("package", fragment.main.packageName, null))
                    fragment.registerFloatingPermission.launch(intent)
                }
                catch (_: Exception) { }
            }
        }

        v.itemOffsetX.value = lyricsSettings.offsetX
        v.testPlaceholder1.setWidthPercent(lyricsSettings.offsetX)

        v.itemOffsetY.value = lyricsSettings.offsetY
        setOffsetY(lyricsSettings.offsetY)

        v.itemWidth.value = lyricsSettings.width
        v.testSurface.setWidthPercent(lyricsSettings.width)

        v.itemTextColor.color = lyricsSettings.textColor
        v.testSurface.textColor = lyricsSettings.textColor

        val backgroundColor = lyricsSettings.backgroundColor

        v.itemBackgroundColor.color = backgroundColor.detachAlpha()
        v.itemBackgroundAlpha.alphaValue = backgroundColor.getAlpha()
        v.testSurface.backgroundColor = backgroundColor

        v.itemOffsetX.addOnChangeListener { _, value, _ ->
            v.testPlaceholder1.setWidthPercent(value)
            v.testSurface.text = "这是一条测试歌词"
        }

        v.itemOffsetX.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.offsetX = v.itemOffsetX.value
                updateSettings()
            }
            false
        }

        v.itemOffsetY.addOnChangeListener { _, value, _ ->
            setOffsetY(value)
            v.testSurface.text = "这是一条测试歌词"
        }

        v.itemOffsetY.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.offsetY = v.itemOffsetY.value
                updateSettings()
            }
            false
        }

        v.itemWidth.addOnChangeListener { _, value, _ ->
            v.testSurface.setWidthPercent(value)
            v.testSurface.text = "这是一条测试歌词"
        }

        v.itemWidth.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.width = v.itemWidth.value
                updateSettings()
            }
            false
        }

        v.itemTextColor.setOnColorChangeListener { _, color ->
            v.testSurface.textColor = color
        }

        v.itemTextColor.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.textColor = v.itemTextColor.color
                updateSettings()
            }
            false
        }

        v.itemBackgroundColor.setOnColorChangeListener { _, color ->
            val alpha = lyricsSettings.backgroundColor.getAlpha()
            v.testSurface.backgroundColor = color.detachAlpha().attachAlpha(alpha)
        }

        v.itemBackgroundColor.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                val alpha = lyricsSettings.backgroundColor.getAlpha()
                lyricsSettings.backgroundColor = v.itemBackgroundColor.color.detachAlpha().attachAlpha(alpha)
                updateSettings()
            }
            false
        }

        v.itemBackgroundAlpha.setOnAlphaChangeListener { _, alpha ->
            val rgb = lyricsSettings.backgroundColor.detachAlpha()
            v.testSurface.backgroundColor = rgb.attachAlpha(alpha)
        }

        v.itemBackgroundAlpha.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                val rgb = lyricsSettings.backgroundColor.detachAlpha()
                lyricsSettings.backgroundColor = rgb.attachAlpha(v.itemBackgroundAlpha.alphaValue)
                updateSettings()
            }
            false
        }
    }

    private fun updateFloatingIconStatus() {
        v.ivFloating.load(if (canShow) R.drawable.icon_yes else R.drawable.icon_no)
    }

    private fun View.setWidthPercent(value: Float) {
        layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
            matchConstraintPercentWidth = value
        }
    }

    private fun setOffsetY(value: Float) {
        v.testContainer.setPadding(0, (value * 100).toInt(), 0, 0)
    }

    private fun updateSettings() {
        Config.music_lyrics_settings = lyricsSettings
        fragment.main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_LYRICS_SETTINGS)
    }
}