package com.dinesh.chat.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Message(
    @SerialName("_id") val id: String = ObjectId().toString(),
    val to: String? = null,
    val from: String? = null,
    val text: String? = null,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val fileName: String? = null,
    @Contextual val timestamp: Long
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
