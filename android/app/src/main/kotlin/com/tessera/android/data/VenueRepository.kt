package com.tessera.android.data

import android.content.Context
import com.tessera.android.data.dto.VenueDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class VenueRepository(context: Context) {

    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun list(): List<VenueDto> = withContext(Dispatchers.IO) {
        JSONObject(fetch("/api/v1/venues?size=200")).getJSONArray("content").mapObjects(::parseVenue)
    }

    suspend fun create(name: String, capacity: Int, address: String?): VenueDto = withContext(Dispatchers.IO) {
        val body = JSONObject().put("name", name).put("capacity", capacity)
            .apply { if (address != null && address.isNotBlank()) put("address", address) }
            .toString()
        parseVenue(JSONObject(mutate(Method.POST, "/api/v1/venues", body)))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/venues/$id", null)
        Unit
    }

    private fun parseVenue(o: JSONObject) = VenueDto(
        id = o.getLong("id"),
        name = o.getString("name"),
        capacity = o.getInt("capacity"),
        address = o.optStringOrNull("address"),
    )

    private suspend fun fetch(path: String): String = mutate(Method.GET, path, null)

    private suspend fun mutate(method: Method, path: String, jsonBody: String?): String {
        val token = keycloak.freshAccessToken()
        var req = Request(method, "${ServerConfig.baseUrl}$path").header("Accept", "application/json")
        if (token != null) req = req.header("Authorization", "Bearer $token")
        if (jsonBody != null) req = req.header("Content-Type", "application/json").body(jsonBody)
        val resp = client(req)
        if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
        return resp.bodyString()
    }
}
