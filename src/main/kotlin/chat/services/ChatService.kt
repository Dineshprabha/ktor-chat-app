package com.dinesh.chat.services

import com.dinesh.chat.model.ChatUserDTO
import com.dinesh.db.table.User
import com.dinesh.db.table.UserChats
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ChatService {

    fun getChatsForUser(userId: UUID): List<ChatUserDTO> {
        return transaction {
            (UserChats innerJoin User)
                .select(UserChats.id, User.id, User.name, User.email, User.imageUrl, UserChats.lastMessage, UserChats.lastMessageTime)
                .where {
                    UserChats.userId eq userId
                }
                .orderBy(UserChats.lastMessageTime to SortOrder.DESC)
                .map {
                    ChatUserDTO(
                        chatId = it[UserChats.id],
                        userId = it[User.id],
                        name = it[User.name],
                        email = it[User.email],
                        imageUrl = it[User.imageUrl],
                        lastMessage = it[UserChats.lastMessage],
                        lastMessageTime = it[UserChats.lastMessageTime]
                    )
                }
        }
    }

}