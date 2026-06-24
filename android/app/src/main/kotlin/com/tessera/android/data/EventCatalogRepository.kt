package com.tessera.android.data

import android.content.Context
import com.tessera.android.data.dto.CatalogEntry
import com.tessera.android.data.dto.ClubDto
import com.tessera.android.data.dto.EventDto
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.VenueDto
import com.tessera.android.screens.components.initialsFromName
import com.tessera.android.screens.components.shortFromName
import com.tessera.android.screens.components.toneForId
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONObject

class EventCatalogRepository(context: Context) {

    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun catalog(): List<CatalogEntry> = withContext(Dispatchers.IO) {
        val token = keycloak.freshAccessToken()
        val events = contentOf("/api/v1/events?size=100", token).mapObjects(::parseEvent)
        val clubs = contentOf("/api/v1/clubs?size=200", token).mapObjects(::parseClub).associateBy { it.id }
        val venues = contentOf("/api/v1/venues?size=200", token).mapObjects(::parseVenue).associateBy { it.id }
        val matches = contentOf("/api/v1/matches?size=200", token).mapObjects(::parseMatch).associateBy { it.id }
        events
            .filter { it.matchId != null }
            .map { ev ->
                val match = ev.matchId?.let { matches[it] }
                buildEntry(ev, match, match?.homeClubId?.let(clubs::get), match?.awayClubId?.let(clubs::get), match?.venueId?.let(venues::get))
            }
            .sortedWith(compareBy(nullsLast()) { it.kickoffAt })
    }

    suspend fun entry(eventId: Long): CatalogEntry = withContext(Dispatchers.IO) {
        val token = keycloak.freshAccessToken()
        val ev = parseEvent(JSONObject(get("/api/v1/events/$eventId", token)))
        val match = ev.matchId?.let { runCatching { parseMatch(JSONObject(get("/api/v1/matches/$it", token))) }.getOrNull() }
        val home = match?.homeClubId?.let { runCatching { parseClub(JSONObject(get("/api/v1/clubs/$it", token))) }.getOrNull() }
        val away = match?.awayClubId?.let { runCatching { parseClub(JSONObject(get("/api/v1/clubs/$it", token))) }.getOrNull() }
        val venue = match?.venueId?.let { runCatching { parseVenue(JSONObject(get("/api/v1/venues/$it", token))) }.getOrNull() }
        buildEntry(ev, match, home, away, venue)
    }

    private fun buildEntry(ev: EventDto, match: MatchDto?, home: ClubDto?, away: ClubDto?, venue: VenueDto?): CatalogEntry {
        val fixture = splitFixture(ev.matchLabel)
        val homeName = home?.name ?: fixture?.first ?: "Casa"
        val awayName = away?.name ?: fixture?.second ?: "Fora"
        return CatalogEntry(
            eventId = ev.id,
            matchId = ev.matchId,
            eventStatus = ev.status,
            homeShort = shortFromName(homeName),
            homeInitials = initialsFromName(homeName),
            homeTone = toneForId(home?.id ?: homeName.hashCode().toLong()),
            awayShort = shortFromName(awayName),
            awayInitials = initialsFromName(awayName),
            awayTone = toneForId(away?.id ?: awayName.hashCode().toLong()),
            venueName = venue?.name,
            venueCapacity = venue?.capacity,
            kickoffAt = match?.kickoffAt,
            matchStatus = match?.status,
            homeScore = match?.homeScore,
            awayScore = match?.awayScore,
            priceNormal = ev.priceNormal,
            priceSupporter = ev.priceSupporter,
        )
    }

    private fun splitFixture(label: String?): Pair<String, String>? {
        if (label.isNullOrBlank()) return null
        val cleaned = label.substringBeforeLast("(").trim()
        listOf(" vs ", " VS ", " x ").forEach { sep ->
            if (cleaned.contains(sep, ignoreCase = true)) {
                val parts = cleaned.split(sep, ignoreCase = true, limit = 2)
                if (parts.size == 2) return parts[0].trim() to parts[1].trim()
            }
        }
        return null
    }

    private fun contentOf(path: String, token: String?) =
        JSONObject(get(path, token)).getJSONArray("content")

    private fun get(path: String, token: String?): String {
        var req = Request(Method.GET, "${ServerConfig.baseUrl}$path").header("Accept", "application/json")
        if (token != null) req = req.header("Authorization", "Bearer $token")
        val resp = client(req)
        if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
        return resp.bodyString()
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

    private fun parseClub(o: JSONObject) = ClubDto(
        id = o.getLong("id"),
        name = o.getString("name"),
        foundedYear = o.optIntOrNull("foundedYear"),
        crestUrl = o.optStringOrNull("crestUrl"),
    )

    private fun parseVenue(o: JSONObject) = VenueDto(
        id = o.getLong("id"),
        name = o.getString("name"),
        capacity = o.getInt("capacity"),
        address = o.optStringOrNull("address"),
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
