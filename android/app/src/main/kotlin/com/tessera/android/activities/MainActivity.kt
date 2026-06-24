package com.tessera.android.activities

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tessera.android.data.KeycloakClient
import com.tessera.android.data.NsdkRepository
import com.tessera.android.data.Sounder
import com.tessera.android.navigation.AppNavigation
import com.tessera.android.shared.ScanState
import com.tessera.android.shared.ServerConfig
import com.tessera.android.ui.theme.TesseraTheme

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "onCreate — initializing NSDK + Sounder")
        ServerConfig.load(this)
        KeycloakClient(this).bootstrap()
        NsdkRepository.init(this)
        Sounder.init()
        setContent {
            TesseraTheme {
                AppNavigation()
            }
        }
    }

    private fun isScanTriggerKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isScanTriggerKey(keyCode) && NsdkRepository.available) {
            if (event?.repeatCount == 0) {
                Log.d(tag, "key DOWN (initial) — scanActive=true")
                ScanState.scanActive.value = true
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isScanTriggerKey(keyCode) && NsdkRepository.available) {
            Log.d(tag, "key UP — scanActive=false")
            ScanState.scanActive.value = false
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        Log.i(tag, "onDestroy — destroying NSDK + Sounder")
        super.onDestroy()
        Sounder.destroy()
        NsdkRepository.destroy()
    }
}
