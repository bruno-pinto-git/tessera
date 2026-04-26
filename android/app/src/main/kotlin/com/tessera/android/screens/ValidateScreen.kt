package com.tessera.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.newland.nsdk.core.api.common.ModuleType
import com.newland.nsdk.core.api.internal.barcodedecoder.DecodingCallback
import com.newland.nsdk.core.api.internal.barcodescanner.BarcodeScanner
import com.newland.nsdk.core.api.internal.barcodescanner.ScanParameters
import com.newland.nsdk.core.internal.NSDKModuleManagerImpl
import android.util.Log
import com.tessera.android.activities.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request

private const val BASE_URL = "http://192.168.1.62:8080"

enum class ValidationState {
    IDLE,
    VALID,
    INVALID
}

@Composable
fun ValidateScreen() {
    val scannedText = remember { mutableStateOf("") }
    val validationState = remember { mutableStateOf(ValidationState.IDLE) }
    val scanRequested = MainActivity.scanRequested

    val barcodeScanner = remember {
        NSDKModuleManagerImpl.getInstance()
            .getModule(ModuleType.BARCODE_SCANNER) as BarcodeScanner
    }

    DisposableEffect(Unit) {
        barcodeScanner.setDecodingCallback(DecodingCallback { eventCode, result ->
            scannedText.value = result?.trim() ?: ""
        })
        barcodeScanner.initScan(ScanParameters())

        onDispose {
            barcodeScanner.stopScan()
            barcodeScanner.releaseScan()
        }
    }

    LaunchedEffect(scanRequested.value) {
        if (scanRequested.value) {
            validationState.value = ValidationState.IDLE
            barcodeScanner.startScan()
            MainActivity.scanRequested.value = false
        }
    }

    LaunchedEffect(scannedText.value) {
        val code = scannedText.value
        if (code.isNotEmpty()) {
            Log.d("ValidateScreen", "Scanned code: $code")
            Log.d("ValidateScreen", "Sending to: $BASE_URL/api/tickets/validate")
            try {
                val status = withContext(Dispatchers.IO) {
                    val client = OkHttp()
                    val body = """{"code": "$code"}"""
                    Log.d("ValidateScreen", "Request body: $body")
                    val request = Request(Method.POST, "$BASE_URL/api/tickets/validate")
                        .header("Content-Type", "application/json")
                        .body(body)
                    val response = client(request)
                    Log.d("ValidateScreen", "Response: ${response.status.code} - ${response.bodyString()}")
                    response.status.code
                }
                validationState.value = if (status == 200) ValidationState.VALID else ValidationState.INVALID
            } catch (e: Exception) {
                Log.e("ValidateScreen", "Request failed: ${e.message}", e)
                validationState.value = ValidationState.INVALID
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Validação de Bilhetes",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (validationState.value) {
                ValidationState.IDLE -> {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Leia um QR Code",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                ValidationState.VALID -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Válido",
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Bilhete Válido",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF4CAF50)
                    )
                }
                ValidationState.INVALID -> {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = "Inválido",
                        modifier = Modifier.size(120.dp),
                        tint = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Bilhete Inválido",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}
