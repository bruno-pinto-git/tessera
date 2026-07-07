package com.tessera.match.seed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tessera.match.club.Club
import com.tessera.match.club.ClubRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
@Profile("!test")
@Order(100)
class ClubsSeeder(
    private val clubRepository: ClubRepository,
    @Value("\${tessera.seed.clubs.enabled:false}") private val enabled: Boolean,
    @Value("\${tessera.seed.clubs.competitions:Q182994,Q754488,Q13668768}")
    private val competitionsCsv: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(ClubsSeeder::class.java)
    private val mapper = jacksonObjectMapper()
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    override fun run(args: ApplicationArguments?) {
        if (!enabled) {
            log.info("Clubs seed skipped (tessera.seed.clubs.enabled=false).")
            return
        }
        val existing = clubRepository.count()
        if (existing > 0) {
            log.info("Clubs seed skipped (table already has $existing rows).")
            return
        }

        val competitions = competitionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        log.info("Seeding clubs from Wikidata. Competitions: $competitions")

        var created = 0
        var skipped = 0

        for (competition in competitions) {
            try {
                val clubs = fetchClubsFor(competition)
                log.info("Wikidata returned ${clubs.size} clubs for $competition.")
                for (entry in clubs) {
                    if (clubRepository.existsActiveByNameIgnoreCase(entry.name)) {
                        skipped++
                        continue
                    }
                    val club = Club(
                        name = entry.name,
                        foundedYear = entry.foundedYear,
                        crestUrl = entry.crestUrl,
                    )
                    clubRepository.save(club)
                    created++
                }
            } catch (e: Exception) {
                log.error("Failed to seed clubs for competition $competition: ${e.message}", e)
            }
        }

        log.info("Clubs seed done. Created: $created, skipped (already existed): $skipped.")
    }

    private fun fetchClubsFor(competitionQId: String): List<WikidataClub> {
        val sparql = """
            SELECT DISTINCT ?club ?clubLabel ?founded ?crest WHERE {
              ?club wdt:P31 wd:Q476028 ;
                    wdt:P17 wd:Q45 ;
                    wdt:P118 wd:$competitionQId .
              OPTIONAL { ?club wdt:P571 ?founded . }
              OPTIONAL { ?club wdt:P154 ?crest . }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "pt,en" . }
            }
            ORDER BY ?clubLabel
        """.trimIndent()

        val uri = URI.create(
            "https://query.wikidata.org/sparql?query=" +
                URLEncoder.encode(sparql, StandardCharsets.UTF_8) +
                "&format=json"
        )

        val request = HttpRequest.newBuilder(uri)
            .header("User-Agent", "Tessera/0.1 (academic-project; ISEL; match-service)")
            .header("Accept", "application/sparql-results+json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw RuntimeException(
                "Wikidata returned ${response.statusCode()}: ${response.body().take(200)}",
            )
        }

        val root = mapper.readTree(response.body())
        val bindings = root.path("results").path("bindings")
        if (!bindings.isArray) return emptyList()

        val unnamed = Regex("^Q\\d+$")
        return bindings.mapNotNull { row ->
            val name = row.path("clubLabel").path("value").asText("").trim()
            if (name.isEmpty() || unnamed.matches(name)) return@mapNotNull null

            val foundedYear = row.path("founded").path("value")
                .asText("")
                .take(4)
                .toIntOrNull()

            val crestUrl = row.path("crest").path("value")
                .asText("")
                .takeIf { it.isNotBlank() }

            WikidataClub(name, foundedYear, crestUrl)
        }
    }

    private data class WikidataClub(
        val name: String,
        val foundedYear: Int?,
        val crestUrl: String?,
    )
}