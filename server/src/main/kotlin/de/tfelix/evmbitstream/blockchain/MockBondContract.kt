package de.tfelix.evmbitstream.blockchain

import org.springframework.stereotype.Component

@Component
class MockBondContract : BondContract {
    override fun isActive(address: String): Boolean {
        return true
    }
}