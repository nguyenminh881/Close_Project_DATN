package com.example.cameraprovider.model

import com.google.firebase.Timestamp

data class Post(
    val postId:String="",
    val userId: String="",
    val userName: String?="",
    val userAvatar: String?="",
    val content: String?="",
    val imageURL: String?="",
    val voiceURL: String?="",
    val createdAt: Timestamp?=null,
    val likes: MutableList<Like> = mutableListOf()
)
data class Like(
    val userId: String = "",
    val reactions: MutableList<String> =mutableListOf()
)