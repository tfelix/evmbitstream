package de.tfelix.evmbitstream.payment

import jakarta.persistence.*
import java.time.Instant

@Entity
class ChannelState(
    @Column(nullable = false)
    val sequenceNumber: Long,

    @Column(nullable = false, unique = true)
    val channelId: String,

    val lockedUntil: Instant,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0
)