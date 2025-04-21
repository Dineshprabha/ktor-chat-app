package com.dinesh.chat.routes

import com.dinesh.chat.model.Message
import com.dinesh.chat.model.SystemMessage
import com.dinesh.chat.services.ChatService
import com.dinesh.chat.services.MongoMessageService
import com.dinesh.utils.Constants
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val activeSessions = ConcurrentHashMap<String, WebSocketSession>()
private val logger = LoggerFactory.getLogger("ktor.chat")

fun Route.chatRoutes(chatService: ChatService, mongoMessageService: MongoMessageService) {

    authenticate("jwt-auth") {

        get(Constants.GET_MESSAGES_BY_ID) {
            val senderId = call.principal<JWTPrincipal>()?.payload?.getClaim("user_id")?.asString()
            val receiverId = call.parameters["receiverId"]

            if (senderId == null || receiverId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing userId or unauthorized.")
                return@get
            }

            val messages = mongoMessageService.getMessagesBetween(senderId, receiverId)
            call.respond(HttpStatusCode.OK, messages)
        }

        get(Constants.GET_CHAT_USERS) {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("user_id")?.asString()

            if (userId == null) {
                call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                return@get
            }

            val chats = chatService.getChatsForUser(UUID.fromString(userId))
            call.respond(chats)
        }

        webSocket("/api/v1/chat") {
            val principal = call.principal<JWTPrincipal>()
            val senderId = principal?.payload?.getClaim("user_id")?.asString()
            val senderEmail = principal?.payload?.getClaim("username")?.asString()

            if (senderId == null || senderEmail == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                return@webSocket
            }

            // Register user session
            activeSessions[senderId] = this

            // Inform user of connection
            send(Json.encodeToString(SystemMessage("✅ Connected as $senderEmail")))

            try {
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val incomingJson = frame.readText()

                        try {
                            val incomingMessage = Json.decodeFromString<Message>(incomingJson)

                            val storedMessage = incomingMessage.copy(
                                from = senderId,
                                timestamp = System.currentTimeMillis() // MongoDB
                            )

                            // 1. ✅ Save message to MongoDB
                            mongoMessageService.insertOneUser(storedMessage)

                            // 2. ✅ Update recent chat in PostgreSQL
                            chatService.upsertUserChat(
                                userId = UUID.fromString(senderId),
                                chatWithId = UUID.fromString(incomingMessage.to),
                                lastMessage = storedMessage.text ?: "",
                                timestamp = CurrentTimestamp
                            )

                            // 3. ✅ Send to receiver if online
                            val receiverSession = activeSessions[incomingMessage.to]
                            receiverSession?.send(Json.encodeToString(Message.serializer(), storedMessage))

                            // Acknowledge sender
                            send(Json.encodeToString(SystemMessage("📨 Delivered to ${incomingMessage.to}")))

                        } catch (e: SerializationException) {
                            logger.error("❌ Invalid message JSON: ${e.message}")
                            send(Json.encodeToString(SystemMessage("❌ Invalid message format.")))
                        } catch (e: Exception) {
                            logger.error("❌ Failed to process message: ${e.message}")
                            send(Json.encodeToString(SystemMessage("❌ Internal error while sending.")))
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
