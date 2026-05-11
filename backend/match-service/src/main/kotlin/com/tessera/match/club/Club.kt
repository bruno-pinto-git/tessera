package com.tessera.match.club

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "club")
class Club(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "founded_year")
    var foundedYear: Int? = null,

    @Column(name = "crest_url", length = 500)
    var crestUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)
