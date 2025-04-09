package com.dinesh.auth.model

data class JWTConfig(
    val realm: String,
    val secret: String,
    val issuer: String,
    val audience: String,
    val tokenExpiry: Long
)
