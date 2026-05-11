package com.tessera.match.team

import jakarta.persistence.*
import java.time.OffsetDateTime

enum class TeamCategory {
    SENIOR_M, SENIOR_F, SUB_23, SUB_19, SUB_17, SUB_15,
    SUB_13, SUB_11, SUB_9, SUB_7, VETERANS, OTHER
}

@Entity
@Table(name = "team")
class Team(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "club_id", nullable = false)
    val clubId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var category: TeamCategory,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)
