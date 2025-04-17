package com.dinesh.db.table

import org.jetbrains.exposed.sql.Table

object UserChats : Table("user_chats") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val userId = uuid("user_id").index()
    val chatWithId = uuid("chat_with_id").index()
    val lastMessage = text("last_message").nullable()
    val lastMessageTime = long("last_message_time").nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    init {
        uniqueIndex(userId, chatWithId)
    }
}
