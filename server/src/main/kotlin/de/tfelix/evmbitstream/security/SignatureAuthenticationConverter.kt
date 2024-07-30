package de.tfelix.evmbitstream.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.web.authentication.AuthenticationConverter
import org.springframework.util.StringUtils
import java.nio.charset.StandardCharsets
import java.util.*

open class SignatureAuthenticationConverter : AuthenticationConverter {

    private val credentialsCharset = StandardCharsets.UTF_8

    override fun convert(request: HttpServletRequest): TransactionSignatureToken? {
        var header = request.getHeader(HttpHeaders.AUTHORIZATION)
            ?: return null
        header = header.trim { it <= ' ' }
        if (!StringUtils.startsWithIgnoreCase(header, AUTHENTICATION_SCHEME_BEARER)) {
            return null
        }
        if (header.equals(AUTHENTICATION_SCHEME_BEARER, ignoreCase = true)) {
            throw BadCredentialsException("Empty basic authentication token")
        }

        val base64Token = header.substring(6).toByteArray(StandardCharsets.UTF_8)
        val decoded = decode(base64Token)
        val token = String(decoded, credentialsCharset)

        val delim = token.indexOf(":")
        if (delim == -1) {
            throw BadCredentialsException("Invalid basic authentication token")
        }

        /*
        Check signature and prefill all you need to know about the token if everything is valid
        val result = UsernamePasswordAuthenticationToken
            .unauthenticated(token.substring(0, delim), token.substring(delim + 1))

        result.details = authenticationDetailsSource.buildDetails(request)*/

        return TransactionSignatureToken("abc")
    }

    private fun decode(base64Token: ByteArray): ByteArray {
        return try {
            Base64.getDecoder().decode(base64Token)
        } catch (ex: IllegalArgumentException) {
            throw BadCredentialsException("Failed to decode basic authentication token")
        }
    }

    companion object {
        const val AUTHENTICATION_SCHEME_BEARER = "Bearer"
    }
}

