package com.dinesh.models

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: Int? = null,
    val name: String,
    val email: String,
    val password: String = ""
)
