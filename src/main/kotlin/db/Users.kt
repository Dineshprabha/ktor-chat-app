package com.dinesh.db

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val password = varchar("password", 255)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val isOnline = bool("is_online").default(false)
    val lastSeen = long("last_seen").default(System.currentTimeMillis()).nullable()

    override val primaryKey = PrimaryKey(id)
}

