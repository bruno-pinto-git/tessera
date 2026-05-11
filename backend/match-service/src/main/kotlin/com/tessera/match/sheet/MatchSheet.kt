package com.tessera.match.sheet

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "match_sheet")
class MatchSheet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "match_id", nullable = false, unique = true)
    val matchId: Long,

    @Column(nullable = false)
    var locked: Boolean = false,

    @Column(name = "locked_at")
    var lockedAt: OffsetDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
