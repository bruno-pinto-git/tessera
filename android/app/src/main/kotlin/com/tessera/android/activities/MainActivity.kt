package com.tessera.android.activities

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tessera.android.navigation.AppNavigation
import com.newland.nsdk.core.internal.NSDKModuleManagerImpl
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {

    companion object {
        var scanRequested = mutableStateOf(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            NSDKModuleManagerImpl.getInstance().init(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to init NSDK: ${e.message}")
        }

        setContent {
            AppNavigation()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            scanRequested.value = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            NSDKModuleManagerImpl.getInstance().destroy()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to destroy NSDK: ${e.message}")
        }
    }
}
