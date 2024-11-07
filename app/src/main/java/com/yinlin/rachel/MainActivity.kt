package com.yinlin.rachel

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.yinlin.rachel.databinding.ActivityMainBinding
import com.yinlin.rachel.model.RachelActivity
import com.yinlin.rachel.model.RachelPages
import com.yinlin.rachel.model.RachelTab

class MainActivity : RachelActivity() {
    private lateinit var v: ActivityMainBinding
     private lateinit var pages: RachelPages

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        enableEdgeToEdge()

        v = ActivityMainBinding.inflate(layoutInflater)
        val view = v.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pages = RachelPages(this, v.btv, arrayOf(
            RachelTab.msg, RachelTab.discovery, RachelTab.music, RachelTab.world, RachelTab.me
        ), RachelTab.msg, R.id.frame)

        runOnUiThread { pages.processIntent(intent) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.apply { pages.processIntent(this) }
    }

    @SuppressLint("MissingSuperCall") @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (pages.goBack()) pages.pop()
    }
}