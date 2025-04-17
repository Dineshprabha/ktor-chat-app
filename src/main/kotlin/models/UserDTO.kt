package com.dinesh.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UserDTO(
    @Contextual val id: UUID? = null,
    val name: String,
    val email: String,
    val username: String? = null,
    val password: String? = null,
    val imageUrl: String? = null,
    val bio : String? = null,
    val isOnline: Boolean = false,
    val phoneNumber : String? = null,
    @Contextual val lastSeen: Long? = null,
)
