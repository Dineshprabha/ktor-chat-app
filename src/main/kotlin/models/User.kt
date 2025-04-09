package com.dinesh.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class User(
    @Contextual val id: UUID = UUID.randomUUID(),
    val name: String,
    val email: String,
    val password: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
