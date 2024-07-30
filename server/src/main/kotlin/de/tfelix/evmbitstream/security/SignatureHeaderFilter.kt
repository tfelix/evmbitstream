package de.tfelix.evmbitstream.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class SignatureHeaderFilter(
    private val authenticationConverter: SignatureAuthenticationConverter,
    private val authenticationManager: AuthenticationManager
) : OncePerRequestFilter() {

    private val securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authRequest = authenticationConverter.convert(request)

            if (authRequest == null) {
                filterChain.doFilter(request, response);
                return
            }

            if (authenticationIsRequired(authRequest)) {
                val authResult = authenticationManager.authenticate(authRequest)
                val context = securityContextHolderStrategy.createEmptyContext()
                context.authentication = authResult;
                securityContextHolderStrategy.context = context
            }
        } catch (ex: AuthenticationException) {
            this.securityContextHolderStrategy.clearContext();
            this.logger.debug("Failed to process authentication request", ex);

            throw ex
        }

        return filterChain.doFilter(request, response)
    }

    private fun authenticationIsRequired(authRequest: TransactionSignatureToken): Boolean {
        val username = authRequest.signature
        // Only reauthenticate if username doesn't match SecurityContextHolder and user
        // isn't authenticated (see SEC-53)
        val existingAuth: Authentication? = this.securityContextHolderStrategy.context.authentication
        return if (existingAuth == null || existingAuth.name != username || !existingAuth.isAuthenticated) {
            true
        } else existingAuth is AnonymousAuthenticationToken
        // Handle unusual condition where an AnonymousAuthenticationToken is already
        // present. This shouldn't happen very often, as BasicProcessingFitler is meant to
        // be earlier in the filter chain than AnonymousAuthenticationFilter.
        // Nevertheless, presence of both an AnonymousAuthenticationToken together with a
        // BASIC authentication request header should indicate reauthentication using the
        // BASIC protocol is desirable. This behaviour is also consistent with that
        // provided by form and digest, both of which force re-authentication if the
        // respective header is detected (and in doing so replace/ any existing
        // AnonymousAuthenticationToken). See SEC-610.
    }
}