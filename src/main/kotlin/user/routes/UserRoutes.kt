package com.dinesh.user.routes

import com.dinesh.db.table.User
import com.dinesh.user.model.UserDTO
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

fun Route.userRoutes() {

    authenticate("jwt-auth") {

        get("api/v1/profile") {
            val principal = call.principal<JWTPrincipal>()
            val username = principal?.payload?.getClaim("username")?.asString()
            val expiresAt = principal?.expiresAt?.time?.minus(System.currentTimeMillis())
            call.respondText("Hello $username, token expires in $expiresAt ms")
        }

        get("api/v1/users") {
            val users = transaction {
                User.selectAll().map {
                    UserDTO(
                        id = it[User.id],
                        name = it[User.name],
                        email = it[User.email],
                        imageUrl = it[User.imageUrl],
                        isOnline = it[User.isOnline],
                    )
                }
            }
            call.respond(users)
        }

        get("api/v1/user/{id}") {
            val userId = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid or missing user ID")

            val user = transaction {
                User
                    .select(User.id, User.name, User.email, User.imageUrl, User.isOnline)
                    .where { User.id eq userId }
                    .map {
                        UserDTO(
                            id = it[User.id],
                            name = it[User.name],
                            email = it[User.email],
                            imageUrl = it[User.imageUrl],
                            isOnline = it[User.isOnline]
                        )
                    }.singleOrNull()
            }


            if (user == null) call.respond(HttpStatusCode.NotFound, "User not found")
            else call.respond(user)
        }
    }
}
