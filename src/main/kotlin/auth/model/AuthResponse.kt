package com.dinesh.auth.model

import com.dinesh.models.UserDTO
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDTO
)
