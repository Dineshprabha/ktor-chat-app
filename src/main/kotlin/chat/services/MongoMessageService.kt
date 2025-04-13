package com.dinesh.chat.services

import com.dinesh.chat.model.Message
import com.dinesh.db.MongoDatabaseFactory
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.eq
import org.litote.kmongo.and
import org.litote.kmongo.or
import org.litote.kmongo.ascending


class MongoMessageService {
    private val db = MongoDatabaseFactory.db
    private val messageCollection = db.getCollection<Message>("chat")

    suspend fun insertOneUser(message: Message) : Boolean {
        return messageCollection.insertOne(message).wasAcknowledged()
    }

    suspend fun getMessagesBetween(senderId: String, receiverId: String): List<Message> {
        return messageCollection.find(
            or(
                and(Message::from eq senderId, Message::to eq receiverId),
                and(Message::from eq receiverId, Message::to eq senderId)
            )
        )
            .sort(ascending(Message::timestamp)) // Optional: order by time
            .toList()
    }

}