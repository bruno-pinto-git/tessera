package com.tessera.ticket.wallet

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.auth.oauth2.ServiceAccountCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.security.interfaces.RSAPrivateKey
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.Base64

private val RFC3339_FORMATTER = DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
    .appendOffset("+HH:MM", "Z")
    .toFormatter()

private fun toRfc3339(raw: String): String =
    runCatching { OffsetDateTime.parse(raw).format(RFC3339_FORMATTER) }.getOrDefault(raw)

private data class TranslatedString(val language: String = "pt-PT", val value: String)
private data class LocalizedString(val defaultValue: TranslatedString)
private data class EventVenue(val name: LocalizedString, val address: LocalizedString)
private data class EventDateTime(val start: String)
private data class Barcode(val type: String = "QR_CODE", val value: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class EventTicketClassPayload(
    val id: String,
    val issuerName: String,
    val eventName: LocalizedString,
    val reviewStatus: String = "UNDER_REVIEW",
    val venue: EventVenue? = null,
    val dateTime: EventDateTime? = null,
)

private data class EventTicketObjectPayload(
    val id: String,
    val classId: String,
    val state: String = "ACTIVE",
    val ticketNumber: String,
    val ticketType: LocalizedString,
    val barcode: Barcode,
)

private data class WalletObjectsPayload(
    val eventTicketClasses: List<EventTicketClassPayload>,
    val eventTicketObjects: List<EventTicketObjectPayload>,
)

private data class WalletJwtPayload(
    val iss: String,
    val payload: WalletObjectsPayload,
    val aud: String = "google",
    val typ: String = "savetowallet",
    val iat: Long = System.currentTimeMillis() / 1000,
    val origins: List<String> = emptyList(),
)

@Component
class GoogleWalletClient(
    @Value("\${tessera.google-wallet.issuer-id:}") private val issuerId: String,
    @Value("\${tessera.google-wallet.service-account-json-base64:}") serviceAccountJsonBase64: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper().apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

    private val credentials: ServiceAccountCredentials? =
        serviceAccountJsonBase64.takeIf { it.isNotBlank() }?.let {
            ServiceAccountCredentials.fromStream(
                ByteArrayInputStream(Base64.getDecoder().decode(it)),
            ) as ServiceAccountCredentials
        }

    fun createSaveUrl(
        ticketCode: String,
        eventId: Long,
        eventTitle: String,
        venue: String?,
        kickoffAt: String?,
        tierLabel: String,
    ): String {
        val creds = credentials ?: error("Google Wallet is not configured (missing service account)")

        val classId = "$issuerId.event-$eventId"
        val objectId = "$issuerId.ticket-$ticketCode"

        val ticketClass = EventTicketClassPayload(
            id = classId,
            issuerName = "Tessera",
            eventName = LocalizedString(TranslatedString(value = eventTitle)),
            venue = venue?.let { EventVenue(LocalizedString(TranslatedString(value = it)), LocalizedString(TranslatedString(value = it))) },
            dateTime = kickoffAt?.let { EventDateTime(start = toRfc3339(it)) },
        )
        val ticketObject = EventTicketObjectPayload(
            id = objectId,
            classId = classId,
            ticketNumber = ticketCode,
            ticketType = LocalizedString(TranslatedString(value = tierLabel)),
            barcode = Barcode(value = ticketCode),
        )
        val jwtPayload = WalletJwtPayload(
            iss = creds.clientEmail,
            payload = WalletObjectsPayload(
                eventTicketClasses = listOf(ticketClass),
                eventTicketObjects = listOf(ticketObject),
            ),
        )

        val algorithm = Algorithm.RSA256(null, creds.privateKey as RSAPrivateKey)
        val token = JWT.create()
            .withPayload(mapper.writeValueAsString(jwtPayload))
            .sign(algorithm)

        log.info("Google Wallet: createSaveUrl ticketCode={} classId={}", ticketCode, classId)
        return "https://pay.google.com/gp/v/save/$token"
    }
}
