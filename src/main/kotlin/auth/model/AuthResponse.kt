package com.dinesh.auth.model

import com.dinesh.models.UserDTO
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val tokenExpiry: Long,
    val refreshToken: String,
    val refreshTokenExpiry: Long,
    val user: UserDTO
)
