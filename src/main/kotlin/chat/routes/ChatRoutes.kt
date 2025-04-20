package com.dinesh.chat.routes

import com.dinesh.chat.model.Message
import com.dinesh.chat.model.UserChatRequest
import com.dinesh.chat.model.UserChatResponse
import com.dinesh.chat.services.ChatService
import com.dinesh.chat.services.MongoMessageService
import com.dinesh.db.table.UserChats
import com.dinesh.utils.Constants
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
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

        webSocket(Constants.WEBSOCKET_CHAT) {
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
                            val currentTime = System.currentTimeMillis().toLong()

                            val storedMessage = incomingMessage.copy(
                                from = senderId,
                                timestamp = currentTime
                            )

                            // 1. ✅ Store message in MongoDB
                            mongoMessageService.insertOneUser(storedMessage)

                            // 2. ✅ Store/update UserChats for sender and receiver in PostgreSQL
                            val senderUUID = UUID.fromString(senderId)
                            val receiverUUID = UUID.fromString(incomingMessage.to)

                            chatService.upsertUserChat(
                                userId = senderUUID,
                                chatWithId = receiverUUID,
                                lastMessage = storedMessage.text!!,
                                timestamp = CurrentTimestamp
                            )

                            // 3. ✅ Send message to receiver (if online)
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
