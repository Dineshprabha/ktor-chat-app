package com.dinesh

import com.dinesh.db.DatabaseFactory
import com.dinesh.auth.model.JWTConfig
import com.dinesh.chat.services.ChatService
import com.dinesh.chat.services.MongoMessageService
import com.dinesh.plugins.configureJWTAuthentication
import com.dinesh.plugins.configureRouting
import com.dinesh.plugins.configureWebsockets
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val mongoMessageService = MongoMessageService()

    val jwt = environment.config.config("ktor.jwt")
    val secret = jwt.property("secret").getString()
    val issuer = jwt.property("issuer").getString()
    val audience = jwt.property("audience").getString()
    val realm = jwt.property("realm").getString()
    val tokenExpiry = jwt.property("tokenExpiry").getString().toLong()
    val refreshTokenExpiry = jwt.property("refreshTokenExpiry").getString().toLong()

    val config = JWTConfig(
        realm = realm,
        audience = audience,
        issuer = issuer,
        tokenExpiry = tokenExpiry,
        refreshTokenExpiry = refreshTokenExpiry,
        secret = secret,

    )

    DatabaseFactory.init()
    val chatService = ChatService()

    configureWebsockets()
    configureJWTAuthentication(config)
    configureSerialization()
    configureRouting(config, mongoMessageService, chatService)

}
