package com.dinesh.chat.model

import com.dinesh.utils.InstantSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class ChatUserDTO(
    @Contextual val chatId: UUID,
    @Contextual val userId: UUID,
    val name: String,
    val email: String,
    val imageUrl: String?,
    val lastMessage: String?,
    @Serializable(with = InstantSerializer::class)
    val lastMessageTime: Instant? // 👈 FIXED!
)