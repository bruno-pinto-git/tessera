package com.tessera.android.data

import android.util.Log
import com.tessera.android.data.dto.EventDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class EventRepository {

    private val tag = "EventRepository"
    private val client = OkHttp()

    suspend fun list(): List<EventDto> = withContext(Dispatchers.IO) {
        val url = "${ServerConfig.baseUrl}/api/v1/events?size=100&sort=createdAt,desc"
        val resp = client(Request(Method.GET, url).header("Accept", "application/json"))
        if (!resp.status.successful) {
            Log.w(tag, "list events failed: ${resp.status.code}")
            throw IllegalStateException("Erro ${resp.status.code}")
        }
        val content = JSONObject(resp.bodyString()).getJSONArray("content")
        (0 until content.length()).map { parseEvent(content.getJSONObject(it)) }
    }

    suspend fun get(id: Long): EventDto = withContext(Dispatchers.IO) {
        val url = "${ServerConfig.baseUrl}/api/v1/events/$id"
        val resp = client(Request(Method.GET, url).header("Accept", "application/json"))
        if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
        parseEvent(JSONObject(resp.bodyString()))
    }

    private fun parseEvent(o: JSONObject) = EventDto(
        id = o.getLong("id"),
        name = o.optStringOrNull("name"),
        matchId = o.optLongOrNull("matchId"),
        priceNormal = o.getDouble("priceNormal"),
        priceSupporter = o.getDouble("priceSupporter"),
        status = o.getString("status"),
        matchLabel = o.optStringOrNull("matchLabel"),
    )
}
