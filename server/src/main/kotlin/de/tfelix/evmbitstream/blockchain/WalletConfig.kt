package de.tfelix.evmbitstream.blockchain

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
data class WalletConfig(
    @Value("\${demo.web3.wallet-private-key}")
    val privateKey: String
) {
    // For extension in test
    companion object
}