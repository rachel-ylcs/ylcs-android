package com.yinlin.rachel.model

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.yinlin.rachel.RachelApplication

open class RachelActivity : AppCompatActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(RachelApplication.initBaseContext(base))

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        RachelApplication.initBaseContext(this)
    }
}