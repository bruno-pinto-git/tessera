package com.tessera.mockmbway.data

import android.util.Log
import com.tessera.mockmbway.data.dto.Amount
import com.tessera.mockmbway.data.dto.MerchantOut
import com.tessera.mockmbway.data.dto.ReturnStatus
import com.tessera.mockmbway.data.dto.WebhookNotification
import com.tessera.mockmbway.shared.PendingPayment
import com.tessera.mockmbway.shared.Resolution
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.util.UUID
import kotlinx.serialization.json.Json

object WebhookSender {

    private const val TAG = "WebhookSender"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun notify(payment: PendingPayment, resolution: Resolution) {
        val callbackUrl = payment.callbackUrl
        if (callbackUrl.isNullOrBlank()) {
            Log.w(TAG, "No callbackUrl for ${payment.transactionId} (resolution=$resolution) — skipping")
            return
        }

        val statusMsg = when (resolution) {
            Resolution.ACCEPTED -> "Success"
            Resolution.DECLINED -> "Declined"
            Resolution.EXPIRED -> "Expired"
        }
        val notification = WebhookNotification(
            returnStatus = ReturnStatus(
                statusCode = "000",
                statusMsg = statusMsg,
            ),
            paymentStatus = statusMsg,
            paymentMethod = "MBWAY",
            transactionID = payment.transactionId,
            amount = Amount(value = payment.amount, currency = payment.currency),
            merchant = MerchantOut(
                terminalId = payment.terminalId,
                merchantTransactionId = payment.merchantTransactionId,
            ),
            paymentType = "PURS",
            notificationID = UUID.randomUUID().toString(),
        )

        try {
            val response = client.post(callbackUrl) {
                contentType(ContentType.Application.Json)
                setBody(notification)
            }
            Log.i(TAG, "callback $callbackUrl ($resolution) → ${response.status.value} ${response.bodyAsText().take(200)}")
        } catch (e: Exception) {
            Log.e(TAG, "callback to $callbackUrl ($resolution) failed: ${e.message}", e)
        }
    }
}
