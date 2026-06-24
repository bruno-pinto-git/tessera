package com.tessera.android.data

import android.content.Context
import com.tessera.android.data.dto.LineupEntryDto
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.MatchSheetDto
import com.tessera.android.data.dto.OccurrenceDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class SheetRepository(context: Context) {

    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun getMatch(matchId: Long): MatchDto = withContext(Dispatchers.IO) {
        parseMatch(JSONObject(fetch("/api/v1/matches/$matchId")))
    }

    suspend fun getSheet(matchId: Long): MatchSheetDto = withContext(Dispatchers.IO) {
        val o = JSONObject(fetch("/api/v1/matches/$matchId/sheet"))
        MatchSheetDto(
            matchId = o.getLong("matchId"),
            locked = o.optBoolean("locked", false),
            lineup = o.getJSONArray("lineup").mapObjects(::parseLineup),
            occurrences = o.getJSONArray("occurrences").mapObjects(::parseOccurrence),
        )
    }

    suspend fun addLineup(matchId: Long, playerId: Long, role: String, shirtNumber: Int?) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("playerId", playerId).put("role", role)
            .apply { if (shirtNumber != null) put("shirtNumber", shirtNumber) }
            .toString()
        mutate(Method.POST, "/api/v1/matches/$matchId/sheet/lineup", body)
        Unit
    }

    suspend fun removeLineup(matchId: Long, playerId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/matches/$matchId/sheet/lineup/$playerId", null)
        Unit
    }

    suspend fun addOccurrence(matchId: Long, minute: Int, type: String, playerId: Long, replacedPlayerId: Long?) =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("minute", minute).put("type", type).put("playerId", playerId)
                .apply { if (replacedPlayerId != null) put("replacedPlayerId", replacedPlayerId) }
                .toString()
            mutate(Method.POST, "/api/v1/matches/$matchId/sheet/occurrences", body)
            Unit
        }

    suspend fun removeOccurrence(matchId: Long, occurrenceId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/matches/$matchId/sheet/occurrences/$occurrenceId", null)
        Unit
    }

    suspend fun lock(matchId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.POST, "/api/v1/matches/$matchId/sheet/lock", "")
        Unit
    }

    suspend fun unlock(matchId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.POST, "/api/v1/matches/$matchId/sheet/unlock", "")
        Unit
    }

    private fun parseLineup(o: JSONObject) = LineupEntryDto(
        playerId = o.getLong("playerId"),
        teamId = o.getLong("teamId"),
        role = o.getString("role"),
        shirtNumber = o.optIntOrNull("shirtNumber"),
    )

    private fun parseOccurrence(o: JSONObject) = OccurrenceDto(
        id = o.getLong("id"),
        minute = o.getInt("minute"),
        type = o.getString("type"),
        playerId = o.getLong("playerId"),
        teamId = o.optLongOrNull("teamId"),
        replacedPlayerId = o.optLongOrNull("replacedPlayerId"),
    )

    private fun parseMatch(o: JSONObject) = MatchDto(
        id = o.getLong("id"),
        homeTeamId = o.getLong("homeTeamId"),
        awayTeamId = o.getLong("awayTeamId"),
        homeClubId = o.optLongOrNull("homeClubId"),
        awayClubId = o.optLongOrNull("awayClubId"),
        venueId = o.optLongOrNull("venueId"),
        kickoffAt = o.getString("kickoffAt"),
        status = o.getString("status"),
        homeScore = o.optIntOrNull("homeScore"),
        awayScore = o.optIntOrNull("awayScore"),
        refereeName = o.optStringOrNull("refereeName"),
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
