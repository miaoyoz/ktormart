package com.ktormart.userservice.plugins

import com.ktormart.userservice.db.DatabaseFactory.dbQuery
import com.ktormart.userservice.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.insert

fun Application.configureRouting() {
    routing {
        // Health check endpoint for Consul
        get("/health") {
            call.respond(HttpStatusCode.OK, "Healthy")
        }
        route("/users") {

            post("/register") {
                val request = call.receive<UserRegisterRequest>()

                // Simple validation
                if (request.password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, "Password should be at least 6 characters long")
                    return@post
                }

                try {
                    val newUserId = dbQuery {
                        Users.insert {
                            it[username] = request.username
                            it[email] = request.email
                            // In a real app, hash the password!
                            it[passwordHash] = "hashed_${request.password}"
                        } get Users.id
                    }

                    val response = UserResponse(
                        id = newUserId.toString(),
                        username = request.username,
                        email = request.email
                    )
                    call.respond(HttpStatusCode.Created, response)

                } catch (e: org.postgresql.util.PSQLException) {
                    // Handle unique constraint violation (e.g., username or email already exists)
                    call.respond(HttpStatusCode.Conflict, "User with this username or email already exists.")
                }
            }
        }
    }
}