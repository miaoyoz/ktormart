package com.ktormart.notificationservice.events

import kotlinx.serialization.Serializable

@Serializable
data class UserRegisteredEvent(
    val userId: String,
    val username: String,
    val email: String
)