package com.dinesh.plugins

import com.dinesh.auth.model.*
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Application.configureRouting(config: JWTConfig) {


    routing {

        authenticate ("jwt-auth") {
            get("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val username = principal?.payload?.getClaim("username")?.asString()
                val expiresAt = principal?.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello $username the token expires at $expiresAt ms.")
            }

            // 🔒 Secure /users endpoint
            get("/users") {
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

            get("/user/{id}") {
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

        }

        post("signup") {
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

        post("login") {
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
                    val token = generateToken(config, request.email)

                    // Create DTO without exposing password
                    val userDTO = UserDTO(
                        id = user[Users.id],
                        name = user[Users.name],
                        email = user[Users.email],
                        avatarUrl = user[Users.avatarUrl],
                        isOnline =  user[Users.isOnline],
                        lastSeen = user[Users.lastSeen]
                    )

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
