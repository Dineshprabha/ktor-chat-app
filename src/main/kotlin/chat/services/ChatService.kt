package com.dinesh.chat.services

import com.dinesh.chat.model.ChatUserDTO
import com.dinesh.db.table.User
import com.dinesh.db.table.UserChats
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ChatService {

    fun getChatsForUser(userId: UUID): List<ChatUserDTO> {
        return transaction {
            UserChats
                .innerJoin(User, { UserChats.chatWithId }, { User.id }) // Explicit join on chatWithId
                .select(
                    UserChats.id,
                    UserChats.lastMessage,
                    UserChats.lastMessageTime,
                    User.id,
                    User.name,
                    User.email,
                    User.imageUrl
                )
                .where {
                    UserChats.userId eq userId // Filter by logged-in user
                }
                .orderBy(UserChats.lastMessageTime to SortOrder.DESC)
                .map { row ->
                    ChatUserDTO(
                        chatId = row[UserChats.id],
                        userId = row[User.id], // This is chatWithId’s info
                        name = row[User.name],
                        email = row[User.email],
                        imageUrl = row[User.imageUrl],
                        lastMessage = row[UserChats.lastMessage],
                        lastMessageTime = row[UserChats.lastMessageTime]
                    )
                }
        }

    }


    fun upsertUserChat(userId: UUID, chatWithId: UUID, lastMessage: String, timestamp: CurrentTimestamp) {
        transaction {
            val updated = UserChats.update(
                where = { (UserChats.userId eq userId) and  (UserChats.chatWithId eq chatWithId) }
            ) {
                it[UserChats.lastMessage] = lastMessage
                it[UserChats.lastMessageTime] = timestamp
                it[UserChats.updatedAt] = CurrentTimestamp
            }

            if (updated == 0) {
                UserChats.insert {
                    it[UserChats.userId] = userId
                    it[UserChats.chatWithId] = chatWithId
                    it[UserChats.lastMessage] = lastMessage
                    it[UserChats.lastMessageTime] = timestamp
                    it[UserChats.createdAt] = CurrentTimestamp
                    it[UserChats.updatedAt] = CurrentTimestamp
                }
            }
        }
    }


}