package com.ktormart.userservice.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table

// Exposed Table Definition
object Users : Table() {
    val id = uuid("id").autoGenerate()
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)

    override val primaryKey = PrimaryKey(id)
}

// Data Transfer Objects (DTOs) for API
@Serializable
data class UserRegisterRequest(val username: String, val email: String, val password: String)

@Serializable
data class UserResponse(val id: String, val username: String, val email: String)