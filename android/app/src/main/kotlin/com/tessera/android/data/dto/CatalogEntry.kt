package com.tessera.android.data.dto

data class CatalogEntry(
    val eventId: Long,
    val matchId: Long?,
    val eventStatus: String,
    val homeShort: String,
    val homeInitials: String,
    val homeTone: Int,
    val awayShort: String,
    val awayInitials: String,
    val awayTone: Int,
    val venueName: String?,
    val venueCapacity: Int?,
    val kickoffAt: String?,
    val matchStatus: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val priceNormal: Double,
    val priceSupporter: Double,
) {
    val priceFrom: Double get() = minOf(priceNormal, priceSupporter)
    val hasResult: Boolean get() = homeScore != null && awayScore != null
}
