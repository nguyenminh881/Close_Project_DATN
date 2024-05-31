package com.example.cameraprovider.model

import java.sql.Timestamp

data class Friendship(
    val uid1: String,
    val uid2: String,
    val state: String,
    val timeStamp: Timestamp
)
