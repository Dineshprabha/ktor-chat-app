package com.dinesh.chat.routes

import com.dinesh.chat.model.Message
import com.dinesh.chat.services.ChatService
import com.dinesh.chat.services.MongoMessageService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val activeSessions = ConcurrentHashMap<String, WebSocketSession>()
private val logger = LoggerFactory.getLogger("ktor.chat")

fun Route.chatRoutes(chatService: ChatService, mongoMessageService: MongoMessageService) {

    authenticate("jwt-auth") {

        get("api/v1/messages/{receiverId}") {
            val senderId = call.principal<JWTPrincipal>()?.payload?.getClaim("user_id")?.asString()
            val receiverId = call.parameters["receiverId"]

            if (senderId == null || receiverId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing userId or unauthorized.")
                return@get
            }

            val messages = mongoMessageService.getMessagesBetween(senderId, receiverId)
            call.respond(HttpStatusCode.OK, messages)
        }

        get("/api/v1/chat/users") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("user_id")?.asString()

            if (userId == null) {
                call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                return@get
            }

            val chats = chatService.getChatsForUser(UUID.fromString(userId))
            call.respond(chats)
        }

        webSocket("/chat") {
            val senderId = call.principal<JWTPrincipal>()?.payload?.getClaim("user_id")?.asString()
            val senderEmail = call.principal<JWTPrincipal>()?.payload?.getClaim("username")?.asString()

            if (senderId == null || senderEmail == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }

            activeSessions[senderId] = this
            send("✅ Connected as $senderEmail")

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        try {
                            val incomingMessage = Json.decodeFromString<Message>(frame.readText())
                            val storedMessage = incomingMessage.copy(
                                from = senderId,
                                timestamp = System.currentTimeMillis()
                            )
                            mongoMessageService.insertOneUser(storedMessage)

                            val receiverSession = activeSessions[incomingMessage.to]
                            receiverSession?.send(Json.encodeToString(Message.serializer(), storedMessage))
                            send("📨 Message delivered to userId: ${incomingMessage.to}")
                        } catch (e: Exception) {
                            logger.error("Error processing message: ${e.message}")
                            send("❌ Error: Invalid message format.")
                        }
                    }
                }
            } finally {
                activeSessions.remove(senderId)
                close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected"))
            }
        }
    }
}
