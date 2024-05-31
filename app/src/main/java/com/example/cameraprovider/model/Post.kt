package com.example.cameraprovider.model

import java.sql.Timestamp

data class Post(
    val userId: Int,
    val userName: String,
    val userAvatar: Int,
    val content: String,
    val imageURL: Int?,
    val voiceURL: String?,
    val createdAt: Timestamp?,
    val likes: Int?
)
