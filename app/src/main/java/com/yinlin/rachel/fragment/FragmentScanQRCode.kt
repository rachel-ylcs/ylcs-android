package com.yinlin.rachel.fragment

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.zxing.Result
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.BaseCameraScan
import com.king.camera.scan.CameraScan.OnScanResultCallback
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.MultiFormatAnalyzer
import com.king.zxing.util.CodeUtils
import com.yinlin.rachel.MainActivity
import com.yinlin.rachel.tool.Tip
import com.yinlin.rachel.annotation.NewThread
import com.yinlin.rachel.data.BackState
import com.yinlin.rachel.databinding.FragmentScanQrcodeBinding
import com.yinlin.rachel.model.RachelFragment
import com.yinlin.rachel.model.RachelPictureSelector
import com.yinlin.rachel.tool.rachelClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FragmentScanQRCode(main: MainActivity)
    : RachelFragment<FragmentScanQrcodeBinding>(main), OnScanResultCallback<Result> {
    private lateinit var cameraScan: BaseCameraScan<Result>

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) cameraScan.startCamera()
        else {
            tip(Tip.WARNING, "未开启相机权限")
            main.pop()
        }
    }

    override fun bindingClass() = FragmentScanQrcodeBinding::class.java

    override fun init() {
        cameraScan = BaseCameraScan<Result>(this, v.preview)
        // 初始化解码配置
        val decodeConfig = DecodeConfig()
        decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS)
            .setFullAreaScan(false).setAreaRectRatio(0.8f)
            .setAreaRectVerticalOffset(0).setAreaRectHorizontalOffset(0)
        cameraScan.setAnalyzer(MultiFormatAnalyzer(decodeConfig))
            .setPlayBeep(true)
            .bindFlashlightView(v.flashlight)
            .setOnScanResultCallback(this)

        v.flashlight.rachelClick {
            val isTorch: Boolean = cameraScan.isTorchEnabled
            cameraScan.enableTorch(!isTorch)
            v.flashlight.isSelected = !isTorch
        }

        v.album.rachelClick {
            RachelPictureSelector.single(main) { parseQRCode(it) }
        }

        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    override fun quit() {
        cameraScan.release()
    }

    override fun back() = BackState.POP

    override fun onScanResultCallback(result: AnalyzeResult<Result>) = processQRCode(result.result.text)

    @NewThread
    private fun parseQRCode(path: String) {
        lifecycleScope.launch {
            val loading = main.loading
            val result = withContext(Dispatchers.IO) { CodeUtils.parseQRCode(path) }
            loading.dismiss()
            if (result != null) processQRCode(result)
            else tip(Tip.WARNING, "未能识别此数据")
        }
    }

    private fun processQRCode(info: String) {
        cameraScan.setAnalyzeImage(false)
        main.pop()
        main.processUri(Uri.parse(info))
    }
}