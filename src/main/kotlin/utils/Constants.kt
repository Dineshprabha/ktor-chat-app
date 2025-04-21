package com.dinesh.utils

object Constants {

    //Authentication Routes
    const val POST_SIGN_UP = "api/v1/auth/signup"
    const val POST_LOGIN = "api/v1/auth/login"

    //User Routes
    const val GET_PROFILE = "api/v1/profile"
    const val GET_USERS = "api/v1/users"
    const val GET_USERS_BY_ID = "api/v1/user/{id}"

    //Chat Routes
    const val GET_MESSAGES_BY_ID =  "api/v1/messages/{receiverId}"
    const val GET_CHAT_USERS =  "/api/v1/chat/users"
    const val POST_CREATE_CHAT_USER =  "/api/v1/chats/create"

    //Websockets
    const val WEBSOCKET_CHAT =  "/api/v1/chat/{chatId}"
}