package com.nguyen.polyglot.model

data class User(
    val id: String,
    val oauthId: String?,
    val username: String,
    val email: String,
    val avatar: String?,
    val dailyWordCount: Int,
    val feedTopics: String,
    val dailyWordTopic: String,
    val notificationEnabled: Boolean,
    val nativeLanguageId: String,
    val appLanguageId: String,
    val createdAt: String
)
