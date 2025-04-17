package com.dinesh.auth.model

data class JWTConfig(
    val realm: String,
    val secret: String,
    val issuer: String,
    val audience: String,
    val tokenExpiry: Long = 86400000,
    val refreshTokenExpiry: Long = 7 * 86400000
)
