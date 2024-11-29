package com.yinlin.rachel.fragment

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.yinlin.rachel.tool.Config
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.R
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.tool.backgroundColor
import com.yinlin.rachel.data.RachelMessage
import com.yinlin.rachel.databinding.FragmentLyricsSettingsBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelImageLoader.load
import com.yinlin.rachel.model.RachelTab
import com.yinlin.rachel.tool.attachAlpha
import com.yinlin.rachel.tool.detachAlpha
import com.yinlin.rachel.tool.getAlpha
import com.yinlin.rachel.tool.rachelClick
import com.yinlin.rachel.tool.textColor
import com.yinlin.rachel.tool.textSizePx

class FragmentLyricsSettings(main: MainActivity) : RachelFragment<FragmentLyricsSettingsBinding>(main) {
    private lateinit var registerFloatingPermission: ActivityResultLauncher<Intent>

    private val canShow: Boolean get() = Settings.canDrawOverlays(context)

    private val lyricsSettings = Config.music_lyrics_settings

    override fun bindingClass() = FragmentLyricsSettingsBinding::class.java

    override fun init() {
        registerFloatingPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updateFloatingIconStatus()
        }

        updateFloatingIconStatus()
        v.floating.rachelClick {
            if (!canShow) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    intent.setData(Uri.fromParts("package", main.packageName, null))
                    registerFloatingPermission.launch(intent)
                }
                catch (_: Exception) { }
            }
        }

        v.itemOffsetLeft.value = lyricsSettings.offsetLeft
        v.testGuideline1.setGuidelinePercent(lyricsSettings.offsetLeft)
        v.itemOffsetLeft.addOnChangeListener { _, value, _ -> v.testGuideline1.setGuidelinePercent(value) }
        v.itemOffsetLeft.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.offsetLeft = v.itemOffsetLeft.value
                updateSettings()
            }
            false
        }

        v.itemOffsetRight.value = lyricsSettings.offsetRight
        v.testGuideline2.setGuidelinePercent(lyricsSettings.offsetRight)
        v.itemOffsetRight.addOnChangeListener { _, value, _ -> v.testGuideline2.setGuidelinePercent(value) }
        v.itemOffsetRight.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.offsetRight = v.itemOffsetRight.value
                updateSettings()
            }
            false
        }

        v.itemOffsetY.value = lyricsSettings.offsetY
        setOffsetY(lyricsSettings.offsetY)
        v.itemOffsetY.addOnChangeListener { _, value, _ -> setOffsetY(value) }
        v.itemOffsetY.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.offsetY = v.itemOffsetY.value
                updateSettings()
            }
            false
        }

        v.itemTextSize.value = lyricsSettings.textSize
        v.testSurface.textSizePx = lyricsSettings.textSize * main.rd(R.dimen.sm)
        v.itemTextSize.addOnChangeListener { _, value, _ -> v.testSurface.textSizePx = value * main.rd(R.dimen.sm) }
        v.itemTextSize.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.textSize = v.itemTextSize.value
                updateSettings()
            }
            false
        }

        v.itemTextColor.color = lyricsSettings.textColor
        v.testSurface.textColor = lyricsSettings.textColor
        v.itemTextColor.setOnColorChangeListener { _, color -> v.testSurface.textColor = color }
        v.itemTextColor.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                lyricsSettings.textColor = v.itemTextColor.color
                updateSettings()
            }
            false
        }

        val backgroundColor = lyricsSettings.backgroundColor
        v.itemBackgroundColor.color = backgroundColor.detachAlpha()
        v.itemBackgroundAlpha.alphaValue = backgroundColor.getAlpha()
        v.testSurface.backgroundColor = backgroundColor
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

    override fun back() = BackState.POP

    private fun updateFloatingIconStatus() {
        v.ivFloating.load(if (canShow) R.drawable.icon_yes else R.drawable.icon_no)
    }

    private fun setOffsetY(value: Float) {
        v.testContainer.setPadding(0, (value * 150).toInt(), 0, 0)
    }

    private fun updateSettings() {
        Config.music_lyrics_settings = lyricsSettings
        main.sendMessage(RachelTab.music, RachelMessage.MUSIC_UPDATE_LYRICS_SETTINGS)
    }
}