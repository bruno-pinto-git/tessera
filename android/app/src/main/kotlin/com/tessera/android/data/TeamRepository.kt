package com.tessera.android.data

import android.content.Context
import com.tessera.android.data.dto.PlayerDto
import com.tessera.android.data.dto.PlayerInput
import com.tessera.android.data.dto.TeamDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class TeamRepository(context: Context) {

    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun getTeam(id: Long): TeamDto = withContext(Dispatchers.IO) {
        val o = JSONObject(fetch("/api/v1/teams/$id"))
        TeamDto(id = o.getLong("id"), clubId = o.getLong("clubId"), category = o.getString("category"))
    }

    suspend fun players(teamId: Long): List<PlayerDto> = withContext(Dispatchers.IO) {
        JSONObject(fetch("/api/v1/teams/$teamId/players?size=100"))
            .getJSONArray("content").mapObjects(::parsePlayer)
    }

    suspend fun createPlayer(teamId: Long, input: PlayerInput): PlayerDto = withContext(Dispatchers.IO) {
        parsePlayer(JSONObject(mutate(Method.POST, "/api/v1/teams/$teamId/players", playerBody(input))))
    }

    suspend fun updatePlayer(playerId: Long, input: PlayerInput): PlayerDto = withContext(Dispatchers.IO) {
        parsePlayer(JSONObject(mutate(Method.PATCH, "/api/v1/players/$playerId", playerBody(input))))
    }

    suspend fun deletePlayer(playerId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/players/$playerId", null)
        Unit
    }

    private fun playerBody(i: PlayerInput): String = JSONObject()
        .put("firstName", i.firstName)
        .put("lastName", i.lastName)
        .put("position", i.position)
        .put("status", i.status)
        .apply {
            i.shirtNumber?.let { put("shirtNumber", it) }
            i.birthdate?.takeIf { it.isNotBlank() }?.let { put("birthdate", it) }
            i.nationality?.takeIf { it.isNotBlank() }?.let { put("nationality", it.uppercase()) }
            i.dominantFoot?.takeIf { it.isNotBlank() }?.let { put("dominantFoot", it) }
            i.height?.let { put("height", it) }
            i.weight?.let { put("weight", it) }
            i.photoUrl?.takeIf { it.isNotBlank() }?.let { put("photoUrl", it) }
        }
        .toString()

    private fun parsePlayer(o: JSONObject) = PlayerDto(
        id = o.getLong("id"),
        teamId = o.getLong("teamId"),
        firstName = o.getString("firstName"),
        lastName = o.getString("lastName"),
        position = o.getString("position"),
        shirtNumber = o.optIntOrNull("shirtNumber"),
        status = o.getString("status"),
        birthdate = o.optStringOrNull("birthdate"),
        nationality = o.optStringOrNull("nationality"),
        dominantFoot = o.optStringOrNull("dominantFoot"),
        height = o.optIntOrNull("height"),
        weight = o.optIntOrNull("weight"),
        photoUrl = o.optStringOrNull("photoUrl"),
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
