//package com.dinesh.repository
//
//import com.dinesh.db.Users
//import com.dinesh.models.User
//import com.dinesh.utils.hashPassword
//import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
//import org.jetbrains.exposed.sql.insert
//import org.jetbrains.exposed.sql.transactions.transaction
//
//class UserRepository {
//
//    fun addUser(user: User): User = transaction {
//        val inserted = Users.insert {
//            it[name] = user.username
//            it[password] = hashPassword(user.password)
//        }
//        User(inserted[Users.id], user.username, user.password)
//    }
//
//    fun getUserByUsername(username: String): User? = transaction {
//        Users.select( Users.username eq username )
//            .map {
//                User(
//                    it[Users.id],
//                    it[Users.username],
//                    it[Users.password]
//                )
//            }.singleOrNull()
//    }
//
//
//
//}