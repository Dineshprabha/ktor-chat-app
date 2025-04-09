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
    val password: String? = null,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    @Contextual val lastSeen: Long? = null,
)
