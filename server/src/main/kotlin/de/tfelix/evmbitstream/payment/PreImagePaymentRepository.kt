package de.tfelix.evmbitstream.payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PreImagePaymentRepository : JpaRepository<PreImagePayment, Long> {
    fun findByHash(hash: String): PreImagePayment?
}