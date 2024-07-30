package de.tfelix.evmbitstream.security

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

// @Component
class SignatureAuthenticationProvider : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        // Create a new one or mark the existing one as authenticated once this has been confirmed.
        return TransactionSignatureToken("abc")
    }

    override fun supports(authentication: Class<*>): Boolean {
        return TransactionSignatureToken::class.java.isAssignableFrom(authentication)
    }
}