package com.dinesh

import com.dinesh.db.DatabaseFactory
import com.dinesh.auth.model.JWTConfig
import com.dinesh.plugins.configureJWTAuthentication
import com.dinesh.plugins.configureRouting
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val jwt = environment.config.config("ktor.jwt")
    val secret = jwt.property("secret").getString()
    val issuer = jwt.property("issuer").getString()
    val audience = jwt.property("audience").getString()
    val realm = jwt.property("realm").getString()
    val tokenExpiry = jwt.property("expiry").getString().toLong()

    val config = JWTConfig(
        realm = realm,
        audience = audience,
        issuer = issuer,
        tokenExpiry = tokenExpiry,
        secret = secret

    )

    DatabaseFactory.init()
    configureJWTAuthentication(config)
    configureRouting(config)
    configureSerialization()



}
