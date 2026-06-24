package com.tessera.android.data

import android.content.Context
import com.tessera.android.data.dto.ClubDto
import com.tessera.android.data.dto.ClubMembershipDto
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.MeDto
import com.tessera.android.data.dto.MemberDto
import com.tessera.android.data.dto.MembersDto
import com.tessera.android.data.dto.TeamDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONArray
import org.json.JSONObject

class ClubRepository(context: Context) {

    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun me(): MeDto = withContext(Dispatchers.IO) {
        val o = JSONObject(fetch("/api/v1/me"))
        MeDto(
            sub = o.getString("sub"),
            username = o.optStringOrNull("username"),
            roles = o.getJSONArray("roles").toStringList(),
            clubMemberships = o.getJSONArray("clubMemberships").mapObjects {
                ClubMembershipDto(clubId = it.getLong("clubId"), role = it.getString("role"))
            },
        )
    }

    suspend fun getClub(id: Long): ClubDto = withContext(Dispatchers.IO) {
        parseClub(JSONObject(fetch("/api/v1/clubs/$id")))
    }

    suspend fun listClubs(): List<ClubDto> = withContext(Dispatchers.IO) {
        JSONObject(fetch("/api/v1/clubs?size=200")).getJSONArray("content").mapObjects(::parseClub)
    }

    suspend fun createClub(name: String, foundedYear: Int?, crestUrl: String?): ClubDto = withContext(Dispatchers.IO) {
        val body = JSONObject().put("name", name)
            .apply {
                if (foundedYear != null) put("foundedYear", foundedYear)
                if (crestUrl != null) put("crestUrl", crestUrl)
            }
            .toString()
        parseClub(JSONObject(mutate(Method.POST, "/api/v1/clubs", body)))
    }

    suspend fun updateClub(id: Long, name: String?, foundedYear: Int?, crestUrl: String?): ClubDto = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .apply {
                if (!name.isNullOrBlank()) put("name", name)
                if (foundedYear != null) put("foundedYear", foundedYear)
                if (!crestUrl.isNullOrBlank()) put("crestUrl", crestUrl)
            }
            .toString()
        parseClub(JSONObject(mutate(Method.PATCH, "/api/v1/clubs/$id", body)))
    }

    suspend fun deleteClub(id: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/clubs/$id", null)
        Unit
    }

    suspend fun members(clubId: Long): MembersDto = withContext(Dispatchers.IO) {
        val o = JSONObject(fetch("/api/v1/clubs/$clubId/members"))
        MembersDto(
            managers = o.getJSONArray("managers").mapObjects(::parseMember),
            staff = o.getJSONArray("staff").mapObjects(::parseMember),
        )
    }

    suspend fun teams(clubId: Long): List<TeamDto> = withContext(Dispatchers.IO) {
        JSONArray(fetch("/api/v1/clubs/$clubId/teams")).mapObjects(::parseTeam)
    }

    suspend fun matchesByClub(clubId: Long): List<MatchDto> = withContext(Dispatchers.IO) {
        val content = JSONObject(fetch("/api/v1/matches?clubId=$clubId&size=100&sort=kickoffAt,desc"))
            .getJSONArray("content")
        content.mapObjects(::parseMatch)
    }

    suspend fun createTeam(clubId: Long, category: String): TeamDto = withContext(Dispatchers.IO) {
        val body = JSONObject().put("category", category).toString()
        parseTeam(JSONObject(mutate(Method.POST, "/api/v1/clubs/$clubId/teams", body)))
    }

    suspend fun updateTeam(teamId: Long, category: String): TeamDto = withContext(Dispatchers.IO) {
        val body = JSONObject().put("category", category).toString()
        parseTeam(JSONObject(mutate(Method.PATCH, "/api/v1/teams/$teamId", body)))
    }

    suspend fun deleteTeam(teamId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/teams/$teamId", null)
        Unit
    }

    suspend fun addExistingMember(clubId: Long, userId: String, role: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("userId", userId).put("role", role).toString()
        mutate(Method.POST, "/api/v1/clubs/$clubId/members", body)
        Unit
    }

    suspend fun addMember(
        clubId: Long,
        username: String,
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        role: String,
    ) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("username", username)
            .put("email", email)
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put("password", password)
            .put("role", role)
            .toString()
        mutate(Method.POST, "/api/v1/clubs/$clubId/members", body)
        Unit
    }

    suspend fun removeMember(clubId: Long, userId: String, role: String) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/clubs/$clubId/members/$userId?role=$role", null)
        Unit
    }

    suspend fun updateMatch(matchId: Long, venueId: Long?, kickoffAt: String, refereeName: String?): MatchDto =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("kickoffAt", kickoffAt)
                .apply {
                    if (venueId != null) put("venueId", venueId)
                    if (!refereeName.isNullOrBlank()) put("refereeName", refereeName)
                }
                .toString()
            parseMatch(JSONObject(mutate(Method.PATCH, "/api/v1/matches/$matchId", body)))
        }

    suspend fun deleteMatch(matchId: Long) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/matches/$matchId", null)
        Unit
    }

    suspend fun openBoxOffice(matchId: Long, name: String, priceNormal: Double, priceSupporter: Double) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("name", name)
                .put("matchId", matchId)
                .put("priceNormal", priceNormal)
                .put("priceSupporter", priceSupporter)
                .put("status", "PUBLISHED")
                .toString()
            mutate(Method.POST, "/api/v1/events", body)
            Unit
        }

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

    private fun parseClub(o: JSONObject) = ClubDto(
        id = o.getLong("id"),
        name = o.getString("name"),
        foundedYear = o.optIntOrNull("foundedYear"),
        crestUrl = o.optStringOrNull("crestUrl"),
    )

    private fun parseMember(o: JSONObject) = MemberDto(
        userId = o.getString("userId"),
        username = o.optStringOrNull("username"),
        email = o.optStringOrNull("email"),
        firstName = o.optStringOrNull("firstName"),
        lastName = o.optStringOrNull("lastName"),
        role = o.getString("role"),
    )

    private fun parseTeam(o: JSONObject) = TeamDto(
        id = o.getLong("id"),
        clubId = o.getLong("clubId"),
        category = o.getString("category"),
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
}
