package com.example.cameraprovider.model

data class Message(
    val senderId: Int,
    val receiverId: Int,
    val message: String,
    val createdAt: String
)
