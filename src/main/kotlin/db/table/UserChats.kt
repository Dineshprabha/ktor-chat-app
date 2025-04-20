package com.dinesh.db.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object UserChats : Table("user_chats") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val userId = uuid("user_id").references(User.id)
    val chatWithId = uuid("chat_with_id").references(User.id)
    val lastMessage = text("last_message").nullable()
    val lastMessageTime = timestamp("last_message_time").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(userId, chatWithId)
    }

    override val primaryKey = PrimaryKey(id)
}
