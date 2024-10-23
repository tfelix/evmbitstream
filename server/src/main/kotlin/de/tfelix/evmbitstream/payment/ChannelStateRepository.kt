package de.tfelix.evmbitstream.payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChannelStateRepository : JpaRepository<ChannelState, Long> {
    fun findByChannelId(channelId: String): ChannelState?
}
