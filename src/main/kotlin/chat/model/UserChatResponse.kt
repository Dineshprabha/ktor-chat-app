package com.dinesh.chat.model

import kotlinx.serialization.Serializable

@Serializable
data class UserChatResponse(
    val chatId: String,
    val userId: String,
    val chatWithId: String,
    val lastMessage: String?,
    val lastMessageTime: Long?,
    val createdAt: String?,
    val updatedAt: String?
)
