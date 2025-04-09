package com.dinesh.plugins

import com.dinesh.db.Users
import com.dinesh.models.AuthRequest
import com.dinesh.models.JWTConfig
import com.dinesh.models.UserDTO
import com.dinesh.utils.PasswordHasher
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureRouting(config: JWTConfig) {

    val usersDB = mutableMapOf<String, String>()

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
        }

        post("signup") {
            val request = call.receive<UserDTO>()
            val existingUser = transaction {
                Users.selectAll().find { it[Users.email] == request.email }
            }

            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, "User already exists")
            } else {
                val hashedPassword = PasswordHasher.hash(request.password)

                transaction {
                    Users.insert {
                        it[name] = request.name
                        it[email] = request.email
                        it[password] = hashedPassword
                    }
                }

                val token = generateToken(config, request.email)
                call.respond(mapOf("token" to token))
            }
        }

        post("login") {
            val request = call.receive<AuthRequest>()

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
                    call.respond(mapOf("token" to token))
                }
            }

        }
    }

//    routing {
//        get("/") {
//            call.respondText("Hello, Ktor with PostgreSQL!")
//        }
//
//        get("/users") {
//            val users = transaction {
//                Users.selectAll().map {
//                    UserDTO(
//                        id = it[Users.id],
//                        name = it[Users.name],
//                        email = it[Users.email],
//                        password = it[Users.password]
//                    )
//                }
//            }
//            call.respond(users)
//        }
//
//        post("/users") {
//            val user = call.receive<UserDTO>()
//            val hashedPassword = PasswordHasher.hash(user.password)
//
//            transaction {
//                Users.insert {
//                    it[name] = user.name
//                    it[email] = user.email
//                    it[password] = hashedPassword
//                }
//            }
//            call.respond(HttpStatusCode.Created, "User registered successfully")
//        }
//    }
}
