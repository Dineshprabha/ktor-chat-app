package com.dinesh.plugins

import com.dinesh.auth.model.*
import com.dinesh.auth.routes.authRoutes
import com.dinesh.chat.routes.chatRoutes
import com.dinesh.chat.services.ChatService
import com.dinesh.chat.services.MongoMessageService
import com.dinesh.user.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*


fun Application.configureRouting(config: JWTConfig, mongoMessageService: MongoMessageService, chatService: ChatService ) {

    routing {
        authRoutes(config)
        userRoutes()
        chatRoutes(chatService, mongoMessageService)

    }
}


