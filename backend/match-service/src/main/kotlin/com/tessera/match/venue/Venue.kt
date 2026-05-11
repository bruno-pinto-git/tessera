package com.tessera.match.venue

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "venue")
class Venue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false)
    var capacity: Int,

    @Column(length = 500)
    var address: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)
