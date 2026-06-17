package com.tessera.ticket.event

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "event")
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "match_id")
    val matchId: Long? = null,

    val name: String? = null,

    /**
     * Snapshot of the fixture ("Home vs Away") captured when the box office is
     * opened. Persists on the event so tickets keep showing the teams even if
     * the match is later deleted in match-service.
     */
    @Column(name = "match_label", length = 255)
    val matchLabel: String? = null,

    /**
     * Home club of the match, snapshotted when the box office is opened. Lets
     * `ticket.ticket.paid` carry the club for per-club sales aggregation without
     * a fragile match-service call at payment time (and survives match deletion).
     */
    @Column(name = "home_club_id")
    val homeClubId: Long? = null,

    @Column(name = "price_normal", nullable = false)
    val priceNormal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "price_supporter", nullable = false)
    val priceSupporter: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val status: String = "DRAFT",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
