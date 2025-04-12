package com.dinesh.chat.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val to: String? = null,
    val from: String? = null,
    val text: String? = null,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val fileName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    GIF,
    MIXED      // Text + Media
}
