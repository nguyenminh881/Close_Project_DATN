package com.example.cameraprovider.model

data class User(
    val UserId: String = "",
    val phoneNumber: String = "",
    val emailUser: String = "",
    val avatarUser: String = "",
    val nameUser: String = "",
    val passwordUser: String = "",
    val friendUser: MutableList<String> = mutableListOf()
)