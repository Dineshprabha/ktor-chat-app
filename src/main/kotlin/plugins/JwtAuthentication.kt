package com.dinesh.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dinesh.auth.model.JWTConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

fun Application.configureJWTAuthentication(config: JWTConfig) {
    install(Authentication) {
        jwt ("jwt-auth") {
            realm = config.realm

            val jwtVerifier = JWT
                .require(Algorithm.HMAC256(config.secret))
                .withAudience(config.audience)
                .withIssuer(config.issuer)
                .build()

            verifier(jwtVerifier)

            validate { jwtCredential ->
            val username = jwtCredential.payload.getClaim("username").asString()
                if (!username.isNullOrBlank()) {
                    JWTPrincipal(jwtCredential.payload)
                }else {
                    null
                }
            }

            challenge { _, _ ->
                call.respondText("Token is not valid or expired", status = HttpStatusCode.Unauthorized)
            }

        }
    }
}

fun generateToken(config: JWTConfig, userId: String, email: String): String {
    return JWT.create()
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .withClaim("user_id", userId)
        .withClaim("username", email)
        .withExpiresAt(Date(System.currentTimeMillis() + config.tokenExpiry))
        .sign(Algorithm.HMAC256(config.secret))
}

fun generateRefreshToken(config: JWTConfig, userId: String, email: String): String {
    return JWT.create()
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .withClaim("user_id", userId)
        .withClaim("username", email)
        .withExpiresAt(Date(System.currentTimeMillis() + config.refreshTokenExpiry))
        .sign(Algorithm.HMAC256(config.secret))
}
