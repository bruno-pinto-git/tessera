package com.tessera.android.data

import android.content.Context
import android.util.Log
import com.tessera.android.data.dto.TicketDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class TicketRepository(context: Context) {

    private val tag = "TicketRepository"
    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun mine(): List<TicketDto> = withContext(Dispatchers.IO) {
        val token = keycloak.freshAccessToken()
        val url = "${ServerConfig.baseUrl}/api/v1/tickets/mine?size=100&sort=createdAt,desc"
        val resp = client(authed(Request(Method.GET, url), token))
        if (!resp.status.successful) {
            Log.w(tag, "mine failed: ${resp.status.code}")
            throw IllegalStateException("Erro ${resp.status.code}")
        }
        val content = JSONObject(resp.bodyString()).getJSONArray("content")
        (0 until content.length()).map { parseTicket(content.getJSONObject(it)) }
    }

    suspend fun get(id: Long): TicketDto = withContext(Dispatchers.IO) {
        val token = keycloak.freshAccessToken()
        val url = "${ServerConfig.baseUrl}/api/v1/tickets/$id"
        val resp = client(authed(Request(Method.GET, url), token))
        if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
        parseTicket(JSONObject(resp.bodyString()))
    }

    suspend fun create(eventId: Long, supporter: Boolean): TicketDto = withContext(Dispatchers.IO) {
        val token = keycloak.freshAccessToken()
        val body = JSONObject().put("eventId", eventId).put("supporter", supporter).toString()
        val url = "${ServerConfig.baseUrl}/api/v1/tickets"
        val resp = client(
            authed(Request(Method.POST, url), token)
                .header("Content-Type", "application/json")
                .body(body),
        )
        if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
        parseTicket(JSONObject(resp.bodyString()))
    }

    suspend fun pay(id: Long, paymentMethod: String, phoneNumber: String?): TicketDto =
        withContext(Dispatchers.IO) {
            val token = keycloak.freshAccessToken()
            val body = JSONObject()
                .put("paymentMethod", paymentMethod)
                .apply { if (phoneNumber != null) put("phoneNumber", phoneNumber) }
                .toString()
            val url = "${ServerConfig.baseUrl}/api/v1/tickets/$id/pay"
            val resp = client(
                authed(Request(Method.POST, url), token)
                    .header("Content-Type", "application/json")
                    .body(body),
            )
            if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
            parseTicket(JSONObject(resp.bodyString()))
        }

    private fun authed(req: Request, token: String?): Request =
        if (token != null) req.header("Authorization", "Bearer $token") else req

    private fun parseTicket(o: JSONObject) = TicketDto(
        id = o.getLong("id"),
        code = o.getString("code"),
        eventId = o.getLong("eventId"),
        matchId = if (o.isNull("matchId")) null else o.getLong("matchId"),
        price = o.getDouble("price"),
        status = o.getString("status"),
        paymentMethod = o.optStringOrNull("paymentMethod"),
        paymentDate = o.optStringOrNull("paymentDate"),
        validationDate = o.optStringOrNull("validationDate"),
        checkoutUrl = o.optStringOrNull("checkoutUrl"),
    )
}
