package com.dinesh.db

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 50).uniqueIndex()
    val name = varchar("name", 50)
    val password = varchar("password", 60)

    override val primaryKey = PrimaryKey(id)
}

