package com.tessera.mockmbway.data

import android.content.Context
import android.util.Log
import com.tessera.mockmbway.data.dto.Amount
import com.tessera.mockmbway.data.dto.CreatePaymentRequest
import com.tessera.mockmbway.data.dto.CreatePaymentResponse
import com.tessera.mockmbway.data.dto.MbwayPurchaseRequest
import com.tessera.mockmbway.data.dto.MbwayPurchaseResponse
import com.tessera.mockmbway.data.dto.MerchantOut
import com.tessera.mockmbway.data.dto.ReturnStatus
import com.tessera.mockmbway.data.dto.StatusResponse
import com.tessera.mockmbway.shared.PendingPayment
import com.tessera.mockmbway.shared.PendingPaymentsState
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.serialization.json.Json

object MockSibsServer {

    private const val TAG = "MockSibsServer"
    const val PORT = 8443

    private var engine: ApplicationEngine? = null
    private var appContext: Context? = null

    fun start(context: Context) {
        if (engine != null) {
            Log.w(TAG, "start() called but server already running")
            return
        }
        appContext = context.applicationContext
        engine = embeddedServer(CIO, port = PORT, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            routing {
                route("/api/v1") {
                    post("/payments") {
                        val req = call.receive<CreatePaymentRequest>()
                        val transactionId = UUID.randomUUID().toString().replace("-", "")
                        val signature = UUID.randomUUID().toString().replace("-", "")

                        val pending = PendingPayment(
                            transactionId = transactionId,
                            transactionSignature = signature,
                            merchantTransactionId = req.merchant.merchantTransactionId,
                            terminalId = req.merchant.terminalId,
                            description = req.transaction.description ?: req.merchant.transactionDescription,
                            amount = req.transaction.amount.value,
                            currency = req.transaction.amount.currency,
                            callbackUrl = req.merchant.callbackUrl,
                        )
                        PendingPaymentsState.pending.add(pending)

                        Log.i(TAG, "Created payment $transactionId for ${req.merchant.merchantTransactionId} (${pending.amount} ${pending.currency})")

                        call.respond(CreatePaymentResponse(
                            returnStatus = ReturnStatus(
                                statusCode = "000",
                                statusMsg = "SUCCESS",
                                statusDescription = "TRANSACTION CREATED SUCCESSFULLY",
                            ),
                            transactionID = transactionId,
                            transactionSignature = signature,
                            amount = Amount(pending.amount, pending.currency),
                            merchant = MerchantOut(
                                terminalId = pending.terminalId,
                                channel = req.merchant.channel,
                                merchantTransactionId = pending.merchantTransactionId,
                            ),
                            paymentMethodList = req.transaction.paymentMethod,
                            expiry = Instant.now().plus(5, ChronoUnit.MINUTES).toString(),
                        ))
                    }

                    post("/payments/{id}/mbway/purchase") {
                        val id = call.parameters["id"].orEmpty()
                        val existing = PendingPaymentsState.find(id)
                        if (existing == null) {
                            call.respond(HttpStatusCode.NotFound, ReturnStatus(
                                statusCode = "999",
                                statusMsg = "TransactionNotFound",
                            ))
                            return@post
                        }
                        val req = call.receive<MbwayPurchaseRequest>()
                        PendingPaymentsState.upsertPhone(id, req.customerPhone)
                        Log.i(TAG, "MB WAY purchase $id phone=${req.customerPhone}")

                        Sounder.incoming()
                        appContext?.let { ctx ->
                            PendingPaymentsState.find(id)?.let { updated ->
                                PaymentNotifications.notify(ctx, updated)
                            }
                        }

                        call.respond(MbwayPurchaseResponse(
                            returnStatus = ReturnStatus(
                                statusCode = "000",
                                statusMsg = "Pending",
                            ),
                            paymentStatus = "Pending",
                            transactionID = id,
                        ))
                    }

                    get("/payments/{id}/status") {
                        val id = call.parameters["id"].orEmpty()
                        val existing = PendingPaymentsState.find(id)
                        if (existing == null) {
                            // Not in pending list — assume already resolved
                            call.respond(StatusResponse(
                                returnStatus = ReturnStatus("000", "Success"),
                                paymentStatus = "Unknown",
                                transactionID = id,
                            ))
                            return@get
                        }
                        call.respond(StatusResponse(
                            returnStatus = ReturnStatus("000", "Success"),
                            paymentStatus = "Pending",
                            transactionID = id,
                        ))
                    }
                }
            }
        }.also { it.start(wait = false) }
        Log.i(TAG, "Mock SIBS server listening on :$PORT")
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 100, timeoutMillis = 500)
        engine = null
        appContext = null
        Log.i(TAG, "Mock SIBS server stopped")
    }
}
