package com.dinesh.chat.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ChatUserDTO(
    @Contextual val chatId: UUID,
    @Contextual val userId: UUID,
    val name: String,
    val email: String,
    val imageUrl: String?,
    val lastMessage: String?,
    @Contextual val lastMessageTime: Long?
)