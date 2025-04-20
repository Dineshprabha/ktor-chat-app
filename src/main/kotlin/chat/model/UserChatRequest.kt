package com.dinesh.chat.model

import kotlinx.serialization.Serializable

@Serializable
data class UserChatRequest(
    val chatWithId: String,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null
)
