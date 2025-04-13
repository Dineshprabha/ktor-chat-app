package com.dinesh.plugins

import com.dinesh.auth.model.*
import com.dinesh.chat.model.Message
import com.dinesh.chat.services.MongoMessageService
import com.dinesh.db.Users
import com.dinesh.models.UserDTO
import com.dinesh.utils.PasswordHasher
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun Application.configureRouting(config: JWTConfig, mongoMessageService: MongoMessageService ) {

    val activeSessions = ConcurrentHashMap<String, WebSocketSession>()
    val logger = LoggerFactory.getLogger("ktor.chat")

    routing {

        authenticate ("jwt-auth") {
            get("api/v1/profile") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.getClaim("username")?.asString()
                val expiresAt = principal?.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello $username the token expires at $expiresAt ms.")
            }

            // 🔒 Secure /users endpoint
            get("api/v1/users") {
                val users = transaction {
                    Users.selectAll().map {
                        UserDTO(
                            id = it[Users.id],
                            name = it[Users.name],
                            email = it[Users.email]
                        )
                    }
                }
                call.respond(users)
            }

            get("api/v1/user/{id}") {
                val userId = call.parameters["id"]?.let { UUID.fromString(it) }

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid or missing user ID")
                    return@get
                }

                val user = transaction {
                    Users.selectAll()
                        .where { Users.id eq userId }
                        .map { row ->
                            UserDTO(
                                id = row[Users.id],
                                name = row[Users.name],
                                email = row[Users.email],
                                avatarUrl = row[Users.avatarUrl],
                                isOnline = row[Users.isOnline],
                                lastSeen = row[Users.lastSeen]
                            )
                        }.singleOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, "User not found")
                } else {
                    call.respond(user)
                }
            }


            get("api/v1/messages/{receiverId}") {
                val principal = call.principal<JWTPrincipal>()
                val senderId = principal?.payload?.getClaim("user_id")?.asString()

                val receiverId = call.parameters["receiverId"]

                if (senderId == null || receiverId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing userId or unauthorized.")
                    return@get
                }

                val messages = mongoMessageService.getMessagesBetween(senderId, receiverId)
                call.respond(HttpStatusCode.OK, messages)
            }



            webSocket("/chat") {
                val principal = call.principal<JWTPrincipal>()
                val senderId = principal?.payload?.getClaim("user_id")?.asString()
                val senderEmail = principal?.payload?.getClaim("username")?.asString()

                if (senderId == null || senderEmail == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                    return@webSocket
                }

                // Register the sender session
                activeSessions[senderId] = this

                send("✅ Connected as $senderEmail")

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            try {
                                val incomingMessage = Json.decodeFromString<Message>(frame.readText())

                                // Don't trust 'from' in the message body; override it with the authenticated user
                                val messageToStore = incomingMessage.copy(
                                    from = senderId,
                                    timestamp = System.currentTimeMillis()
                                )

                                // Save message to MongoDB
                                mongoMessageService.insertOneUser(messageToStore)

                                // Deliver message to receiver if online
                                val receiverSession = activeSessions[incomingMessage.to]
                                if (receiverSession != null) {
                                    val messageJson = Json.encodeToString(Message.serializer(), messageToStore)
                                    receiverSession.send(messageJson)
                                }

                                // Confirm to sender
                                send("📨 Message delivered to userId: ${incomingMessage.to}")
                            } catch (e: Exception) {
                                logger.error("Error processing message: ${e.message}")
                                send("❌ Error: Invalid message format or content.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("WebSocket Error: ${e.localizedMessage}")
                } finally {
                    // Remove the session when disconnected
                    activeSessions.remove(senderId)
                    close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected"))
                }
            }




        }

        post("api/v1/auth/signup") {
            val request = call.receive<AuthRequest>()
            val existingUser = transaction {
                Users.selectAll().find { it[Users.email] == request.email }
            }

            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, "User already exists")
            } else {
                val hashedPassword = PasswordHasher.hash(request.password!!)

                transaction {
                    Users.insert {
                        it[name] = request.name
                        it[email] = request.email
                        it[password] = hashedPassword
                    }
                }

//                val token = generateToken(config, request.email)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "message" to "Register successful"
                    )
                )
            }
        }

        post("api/v1/auth/login") {
            val request = call.receive<LoginRequest>()

            val user = transaction {
                Users.selectAll()
                    .find { it[Users.email] == request.email }
            }

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User not found")
            } else {
                val hashedPassword = user[Users.password]
                val isPasswordValid = PasswordHasher.verify(request.password, hashedPassword)

                if (!isPasswordValid) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                } else {

                    // Create DTO without exposing password
                    val userDTO = UserDTO(
                        id = user[Users.id],
                        name = user[Users.name],
                        email = user[Users.email],
                        avatarUrl = user[Users.avatarUrl],
                        isOnline =  user[Users.isOnline],
                        lastSeen = user[Users.lastSeen]
                    )

                    val token = generateToken(config, userDTO.id.toString(), userDTO.email)

                    val authResponse = AuthResponse(
                        token = token,
                        user = userDTO
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            message = "Login successful",
                            data = authResponse
                        )
                    )

                }
            }

        }
    }
}


