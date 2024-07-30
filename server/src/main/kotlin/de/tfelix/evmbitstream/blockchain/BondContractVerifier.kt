package de.tfelix.evmbitstream.blockchain

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

private val log = KotlinLogging.logger { }

@Component
class BondContractVerifier(
    private val bondContract: BondContract,
    private val wallet: Wallet
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        log.info { "Checking if server bond is available..." }
        if (!bondContract.isActive(wallet.address())) {
            log.error { "Server is not registered in the bond contract, or was slashed. Can not serve files." }
            exitProcess(1)
        }
        log.info { "Server bond is available. Can serve files." }
    }
}