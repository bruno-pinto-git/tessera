package com.tessera.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newland.nsdk.core.api.internal.barcodedecoder.DecodingCallback
import com.newland.nsdk.core.api.internal.barcodescanner.ScanParameters
import com.tessera.android.data.KeycloakClient
import com.tessera.android.data.NsdkRepository
import com.tessera.android.data.Sounder
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

sealed interface ValidationState {
    data object Idle : ValidationState
    data object Valid : ValidationState
    data class Invalid(val reason: InvalidReason) : ValidationState
}

enum class InvalidReason {
    ALREADY_USED,
    NOT_FOUND,
    BAD_CODE,
    AUTH,
    NETWORK,
    UNKNOWN,
}

class ValidateViewModel(application: Application) : AndroidViewModel(application) {

    var validationState: ValidationState by mutableStateOf(ValidationState.Idle, neverEqualPolicy())
        private set

    private val scanner = NsdkRepository.barcodeScanner
    private val keycloak = KeycloakClient(application)
    private var resetJob: Job? = null

    init {
        Log.i(TAG, "init: registering decoding callback + initScan")
        try {
            scanner.setDecodingCallback(DecodingCallback { eventCode, result ->
                Log.d(TAG, "Decode callback: eventCode=$eventCode, result=$result")
                val code = result?.trim().orEmpty()
                if (code.isEmpty()) {
                    Log.d(TAG, "Decode callback: empty result — ignoring")
                    return@DecodingCallback
                }
                if (!UUID_REGEX.matches(code)) {
                    Log.w(TAG, "Decode callback: non-UUID result, dropping: $code")
                    return@DecodingCallback
                }
                onCodeScanned(code)
            })
            val params = ScanParameters().apply {
                setSoundSwitcher(true)
                setTimeout(25400)
            }
            scanner.initScan(params)
            Log.i(TAG, "init: scanner ready")
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
        }
    }

    fun startScan() {
        Log.i(TAG, "startScan() called")
        resetJob?.cancel()
        validationState = ValidationState.Idle
        try {
            scanner.startScan()
            Log.i(TAG, "startScan() -> scanner.startScan() ok")
        } catch (e: Exception) {
            Log.e(TAG, "startScan failed", e)
        }
    }

    fun stopScan() {
        Log.i(TAG, "stopScan() called")
        try {
            scanner.stopScan()
        } catch (e: Exception) {
            Log.d(TAG, "stopScan (already stopped?): ${e.message}")
        }
    }

    private fun onCodeScanned(code: String) {
        Log.d(TAG, "Scanned code: $code")
        viewModelScope.launch {
            val result = try {
                val statusCode = callValidate(code)
                mapStatusToState(statusCode)
            } catch (e: Exception) {
                Log.e(TAG, "Request failed", e)
                ValidationState.Invalid(InvalidReason.NETWORK)
            }
            setResult(result)
        }
    }

    /**
     * Acquires a fresh Keycloak token (refreshes if expiring) and POSTs to
     * `/api/v1/tickets/validate`. Returns the HTTP status code so the caller
     * maps it to a UI state.
     */
    private suspend fun callValidate(code: String): Int {
        val token = keycloak.freshAccessToken()
        return withContext(Dispatchers.IO) {
            val client = OkHttp()
            val body = """{"code": "$code"}"""
            sendValidate(client, body, token)
        }
    }

    private fun sendValidate(client: HttpHandler, body: String, token: String?): Int {
        val req = Request(Method.POST, "${ServerConfig.baseUrl}/api/v1/tickets/validate")
            .header("Content-Type", "application/json")
            .let { if (token != null) it.header("Authorization", "Bearer $token") else it }
            .body(body)
        val resp = client(req)
        Log.d(TAG, "Response: ${resp.status.code} - ${resp.bodyString().take(200)}")
        return resp.status.code
    }

    private fun mapStatusToState(statusCode: Int): ValidationState = when (statusCode) {
        200 -> ValidationState.Valid
        400 -> ValidationState.Invalid(InvalidReason.BAD_CODE)
        401, 403 -> ValidationState.Invalid(InvalidReason.AUTH)
        404 -> ValidationState.Invalid(InvalidReason.NOT_FOUND)
        409 -> ValidationState.Invalid(InvalidReason.ALREADY_USED)
        else -> ValidationState.Invalid(InvalidReason.UNKNOWN)
    }

    private fun setResult(state: ValidationState) {
        validationState = state
        playResultTone(state)
        resetJob?.cancel()
        resetJob = viewModelScope.launch {
            delay(RESULT_DISPLAY_MS)
            validationState = ValidationState.Idle
        }
    }

    private fun playResultTone(state: ValidationState) {
        when (state) {
            ValidationState.Valid -> Sounder.success()
            is ValidationState.Invalid -> when (state.reason) {
                InvalidReason.ALREADY_USED,
                InvalidReason.NOT_FOUND,
                InvalidReason.NETWORK -> Sounder.warning()
                else -> Sounder.error()
            }
            ValidationState.Idle -> Unit
        }
    }

    override fun onCleared() {
        Log.i(TAG, "onCleared() — releasing scanner + keycloak client")
        super.onCleared()
        try {
            scanner.releaseScan()
        } catch (e: Exception) {
            Log.d(TAG, "releaseScan: ${e.message}")
        }
        keycloak.dispose()
    }

    private companion object {
        const val TAG = "ValidateViewModel"
        const val RESULT_DISPLAY_MS = 5_000L
        val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        )
    }
}
