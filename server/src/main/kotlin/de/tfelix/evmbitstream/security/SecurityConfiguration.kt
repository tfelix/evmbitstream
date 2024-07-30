package de.tfelix.evmbitstream.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
// @EnableWebSecurity
// @EnableMethodSecurity
class SecurityConfiguration {

    /*
    @Bean
    fun filterChain(http: HttpSecurity, authenticationManager: AuthenticationManager): SecurityFilterChain {
        val signatureAuthConverter = AlwaysAuthenticatedAuthenticationConverter()

        return http.addFilterAt(
            SignatureHeaderFilter(
                authenticationConverter = signatureAuthConverter,
                authenticationManager = authenticationManager
            ),
            BasicAuthenticationFilter::class.java
        ).sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }.authorizeHttpRequests { authorize ->
            authorize.anyRequest().permitAll()
        }.httpBasic {

        }.build()
    }

    @Bean
    fun authenticationManager(): ProviderManager {
        return ProviderManager(SignatureAuthenticationProvider())
    }

     */
}