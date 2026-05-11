package com.tessera.match.player

import jakarta.persistence.*
import java.time.LocalDate
import java.time.OffsetDateTime

enum class PlayerPosition { GK, DF, MF, FW }
enum class DominantFoot { LEFT, RIGHT, BOTH }
enum class PlayerStatus { ACTIVE, INJURED, SUSPENDED }

@Entity
@Table(name = "player")
class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "team_id", nullable = false)
    val teamId: Long,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    var birthdate: LocalDate? = null,

    @Column(length = 3)
    var nationality: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    var position: PlayerPosition,

    @Column(name = "shirt_number")
    var shirtNumber: Int? = null,

    @Column(name = "photo_url", length = 500)
    var photoUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "dominant_foot", length = 5)
    var dominantFoot: DominantFoot? = null,

    var height: Int? = null,
    var weight: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PlayerStatus = PlayerStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)
