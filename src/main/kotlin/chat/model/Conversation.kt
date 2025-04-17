package com.dinesh.chat.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Conversation(
    @SerialName("_id") val id: String = ObjectId().toString(),

    val participants: List<String>,

    val lastMessage: LastMessage? = null,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class LastMessage(
    val senderId: String,
    val message: String,
    val timestamp: Long
)