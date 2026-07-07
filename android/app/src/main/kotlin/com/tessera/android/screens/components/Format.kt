package com.tessera.android.screens.components

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val PT = Locale("pt", "PT")
private val kickoffFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
private val dayFmt = DateTimeFormatter.ofPattern("EEE", PT)
private val dateFmt = DateTimeFormatter.ofPattern("dd/MM", PT)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", PT)

fun formatKickoff(iso: String): String = try {
    OffsetDateTime.parse(iso).format(kickoffFormatter)
} catch (e: Exception) {
    iso
}

data class KickoffParts(val day: String, val date: String, val time: String)

fun kickoffParts(iso: String?): KickoffParts? {
    if (iso.isNullOrBlank()) return null
    return try {
        val odt = OffsetDateTime.parse(iso)
        KickoffParts(
            day = odt.format(dayFmt).trim('.').replaceFirstChar { it.uppercase() },
            date = odt.format(dateFmt),
            time = odt.format(timeFmt),
        )
    } catch (e: Exception) {
        null
    }
}

fun formatKickoffFull(iso: String?): String {
    val p = kickoffParts(iso) ?: return "Data por confirmar"
    return "${p.day}, ${p.date} · ${p.time}"
}

fun formatShortDateTime(iso: String?): String? {
    val p = kickoffParts(iso) ?: return null
    return "${p.date} ${p.time}"
}

private val localDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val localTime = DateTimeFormatter.ofPattern("HH:mm")

fun isoToLocalDate(iso: String?): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(localDate)
} catch (e: Exception) {
    ""
}

fun isoToLocalTime(iso: String?): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(localTime)
} catch (e: Exception) {
    ""
}

fun localDateTimeToIso(date: String, time: String): String? = try {
    val t = if (time.length == 5) time else "00:00"
    LocalDateTime.parse("${date}T$t").atZone(ZoneId.systemDefault()).toInstant().toString()
} catch (e: Exception) {
    null
}

fun formatEur(value: Double): String = String.format(Locale.US, "%.2f", value).replace('.', ',') + " €"

fun formatCapacity(capacity: Int): String = String.format(PT, "%,d", capacity) + " lugares"

fun prettyEventStatus(status: String): String = when (status) {
    "PUBLISHED" -> "Aberta"
    "SALES_CLOSED" -> "Encerrada"
    "DRAFT" -> "Rascunho"
    "CANCELLED" -> "Cancelada"
    else -> status
}

private val CREST_STOPWORDS = setOf(
    "fc", "sc", "ad", "cd", "gd", "ac", "ud", "af", "sad", "su",
    "de", "da", "do", "dos", "das", "e", "del",
    "clube", "futebol", "sport", "sporting", "associação", "desportiva", "desportivo", "união", "grupo",
)

private fun significantWords(name: String): List<String> =
    name.split(" ", "-").map { it.trim() }.filter { it.isNotBlank() && it.lowercase().trim('.') !in CREST_STOPWORDS }

fun shortFromName(name: String): String {
    val w = significantWords(name)
    return if (w.isEmpty()) name.trim() else w.take(2).joinToString(" ")
}

fun initialsFromName(name: String): String {
    val w = significantWords(name)
    val ini = w.take(3).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    return ini.ifBlank { name.trim().take(2).uppercase() }
}

fun toneForId(id: Long?): Int = if (id == null) 4 else (abs(id) % 6).toInt()

val TEAM_CATEGORIES = listOf(
    "SENIOR_M", "SENIOR_F", "SUB_23", "SUB_19", "SUB_17", "SUB_15",
    "SUB_13", "SUB_11", "SUB_9", "SUB_7", "VETERANS", "OTHER",
)

val PLAYER_POSITIONS = listOf("GK", "DF", "MF", "FW")
val PLAYER_STATUSES = listOf("ACTIVE", "INJURED", "SUSPENDED")
val PLAYER_FEET = listOf("LEFT", "RIGHT", "BOTH")
val CLUB_ROLES = listOf("MANAGER", "STAFF")

fun prettyPosition(position: String): String = when (position) {
    "GK" -> "Guarda-redes"
    "DF" -> "Defesa"
    "MF" -> "Médio"
    "FW" -> "Avançado"
    else -> position
}

fun prettyFoot(foot: String): String = when (foot) {
    "LEFT" -> "Esquerdo"
    "RIGHT" -> "Direito"
    "BOTH" -> "Ambidestro"
    else -> foot
}
val USER_CREATE_ROLES = listOf("club-manager", "staff")

fun prettyClubRole(role: String): String = when (role) {
    "MANAGER" -> "Gestor"
    "STAFF" -> "Staff"
    else -> role
}

fun prettyUserRole(role: String): String = when (role) {
    "platform-admin" -> "Administrador"
    "club-manager" -> "Gestor de clube"
    "staff" -> "Staff"
    "fan" -> "Adepto"
    else -> role
}

fun prettyCategory(category: String): String = when (category) {
    "SENIOR_M" -> "Sénior Masculina"
    "SENIOR_F" -> "Sénior Feminina"
    "VETERANS" -> "Veteranos"
    "OTHER" -> "Outra"
    else -> category.replace("SUB_", "Sub-")
}

fun prettyPlayerStatus(status: String): String = when (status) {
    "ACTIVE" -> "Activo"
    "INJURED" -> "Lesionado"
    "SUSPENDED" -> "Suspenso"
    else -> status
}

val LINEUP_ROLES = listOf("STARTER", "SUBSTITUTE")
val OCCURRENCE_TYPES = listOf("GOAL", "OWN_GOAL", "YELLOW_CARD", "RED_CARD", "SUBSTITUTION", "FOUL")

fun prettyLineupRole(role: String): String = when (role) {
    "STARTER" -> "Titular"
    "SUBSTITUTE" -> "Suplente"
    else -> role
}

fun prettyOccurrenceType(type: String): String = when (type) {
    "GOAL" -> "Golo"
    "OWN_GOAL" -> "Autogolo"
    "YELLOW_CARD" -> "Cartão amarelo"
    "RED_CARD" -> "Cartão vermelho"
    "SUBSTITUTION" -> "Substituição"
    "FOUL" -> "Falta"
    else -> type
}

fun occurrenceIcon(type: String): String = when (type) {
    "GOAL" -> "⚽"
    "OWN_GOAL" -> "🥅"
    "YELLOW_CARD" -> "🟨"
    "RED_CARD" -> "🟥"
    "SUBSTITUTION" -> "🔁"
    "FOUL" -> "⚠️"
    else -> "•"
}

fun prettyMatchStatus(status: String): String = when (status) {
    "SCHEDULED" -> "Agendado"
    "LIVE" -> "Em direto"
    "FINISHED" -> "Terminado"
    "POSTPONED" -> "Adiado"
    "ABANDONED" -> "Abandonado"
    "CANCELLED" -> "Cancelado"
    else -> status
}
