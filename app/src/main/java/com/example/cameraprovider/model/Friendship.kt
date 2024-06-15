package com.example.cameraprovider.model

data class Friendship(
    val id:String?="",
    val uid1: String?="",
    val uid2: String?="",
    val state: String?="",
    val timeStamp: com.google.firebase.Timestamp
)
