package com.dinesh.db

import com.mongodb.kotlin.client.coroutine.MongoClient

object MongoDatabaseFactory {

    private val connectionString = System.getenv("MONGO_DB_URI")

    val db = MongoClient
        .create(connectionString)
        .getDatabase("ktor_mongodb")
}