package com.tessera.mockmbway.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.tessera.mockmbway.data.MockSibsServer
import com.tessera.mockmbway.data.NetworkInfo
import com.tessera.mockmbway.screens.PaymentsScreen
import com.tessera.mockmbway.services.ServerService
import com.tessera.mockmbway.shared.PendingPaymentsState
import com.tessera.mockmbway.ui.theme.MockMbwayTheme

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Log.i(tag, "POST_NOTIFICATIONS granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "onCreate — launching foreground ServerService")
        requestNotificationsPermissionIfNeeded()
        ServerService.start(this)
        PendingPaymentsState.serverEndpoint.value = formatEndpoint()
        setContent {
            MockMbwayTheme {
                PaymentsScreen()
            }
        }
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun formatEndpoint(): String {
        val ip = NetworkInfo.localIPv4() ?: "<no-network>"
        return "http://$ip:${MockSibsServer.PORT}"
    }
}
