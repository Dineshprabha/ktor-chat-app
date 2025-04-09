//package com.dinesh.routes
//
//import com.auth0.jwt.JWT
//import com.auth0.jwt.algorithms.Algorithm
//import com.dinesh.db.Users
//import com.dinesh.auth.model.AuthRequest
//import io.ktor.server.auth.*
//import io.ktor.server.auth.jwt.*
//import io.ktor.server.request.*
//import io.ktor.server.response.*
//import io.ktor.server.routing.*
//import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
//import org.jetbrains.exposed.sql.insert
//import org.jetbrains.exposed.sql.transactions.transaction
//
//data class AuthRoutes (
//    val username: String,
//    val password : String
//)
//
//fun Route.authRoutes(jwtSecret: String, issuer: String, audience: String) {
//    post("/signup") {
//        val req = call.receive<AuthRequest>()
//
//        transaction {
//            Users.insert {
//                it[username] = req.username
//                it[password] = req.password
//            }
//        }
//        call.respond(mapOf("message" to "User created"))
//    }
//
//
//    post("/login") {
//        val req = call.receive<AuthRequest>()
//        val user = transaction {
//            Users.select (Users.username eq req.username)
//                .map { it[Users.username] to it[Users.password] }
//                .singleOrNull()
//        }
//
//        if (user != null && user.second == req.password) {
//            val token = JWT.create()
//                .withAudience(audience)
//                .withIssuer(issuer)
//                .withClaim("username", req.username)
//                .sign(Algorithm.HMAC256(jwtSecret))
//
//            call.respond(mapOf("token" to token))
//        }else {
//            call.respondText("Invalid credentials")
//        }
//    }
//
//    authenticate ("auth-jwt") {
//        get("/user") {
//            val principal = call.principal<JWTPrincipal>()
//            val username = principal?.getClaim("username", String::class)
//            call.respond(mapOf("user" to username))
//        }
//    }
//}