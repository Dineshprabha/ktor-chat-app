package com.dinesh.auth.routes

import com.dinesh.auth.model.*
import com.dinesh.db.table.User
import com.dinesh.plugins.generateRefreshToken
import com.dinesh.plugins.generateToken
import com.dinesh.user.model.UserDTO
import com.dinesh.utils.PasswordHasher
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.authRoutes(config: JWTConfig) {

    post("api/v1/auth/signup") {
        val request = call.receive<AuthRequest>()

        val existingUser = transaction {
            User.selectAll().find { it[User.email] == request.email }
        }

        if (existingUser != null) {
            call.respond(HttpStatusCode.Conflict, "User already exists")
        } else {
            val hashedPassword = PasswordHasher.hash(request.password!!)
            transaction {
                User.insert {
                    it[name] = request.name
                    it[email] = request.email
                    it[password] = hashedPassword
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Register successful"))
        }
    }

    post("api/v1/auth/login") {
        val request = call.receive<LoginRequest>()
        val user = transaction {
            User.selectAll().find { it[User.email] == request.email }
        }

        if (user == null) {
            call.respond(HttpStatusCode.NotFound, "User not found")
        } else {
            val isPasswordValid = PasswordHasher.verify(request.password, user[User.password])
            if (!isPasswordValid) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
            } else {
                val userId = user[User.id]
                val email = user[User.email]
                val now = System.currentTimeMillis()

                val token = generateToken(config, userId.toString(), email)
                val refreshToken = generateRefreshToken(config, userId.toString(), email)

                transaction {
                    User.update({ User.id eq userId }) {
                        it[User.token] = token
                        it[User.refresh_token] = refreshToken
                        it[User.isOnline] = true
                        it[User.lastLoginAt] = CurrentTimestamp
                    }
                }

                val userDTO = UserDTO(
                    id = user[User.id],
                    name = user[User.name],
                    email = user[User.email],
                    username = user[User.username],
                    imageUrl = user[User.imageUrl],
                    bio = user[User.bio],
                    isOnline = true
                )

                val authResponse = AuthResponse(
                    token = token,
                    tokenExpiry = now + config.tokenExpiry,
                    refreshToken = refreshToken,
                    refreshTokenExpiry = now + config.refreshTokenExpiry,
                    user = userDTO
                )

                call.respond(HttpStatusCode.OK, ApiResponse("Login successful", authResponse))
            }
        }
    }
}
