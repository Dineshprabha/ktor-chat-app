package com.dinesh.chat.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemMessage(val info: String)