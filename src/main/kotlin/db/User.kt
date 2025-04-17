package com.dinesh.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object User : Table("users") {

    val id = uuid("id").autoGenerate().uniqueIndex()
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 255).uniqueIndex().nullable()
    val password = varchar("password", 255)
    val token = varchar("token", 512).nullable()
    val refresh_token = varchar("refresh_token", 512).nullable()
    val imageUrl = varchar("image_url", 512).nullable()
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)
    val isOnline = bool("is_online").default(false)
    val bio = text("bio").nullable()
    val phoneNumber = varchar("phone_number", 20).nullable()
    val country = varchar("country", 100).nullable()
    val language = varchar("language", 10).default("en")
    val timezone = varchar("timezone", 100).nullable()
    val loginMethod = varchar("login_method", 50).default("email")
    val lastLoginAt = timestamp("last_login_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}

