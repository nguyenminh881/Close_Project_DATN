package com.example.cameraprovider.model

data class User(
    var UserId: String = "",
    val phoneNumber: String = "",
    val emailUser: String = "",
    var avatarUser: String = "",
    var nameUser: String = "",
    val passwordUser: String = "",
    val token: String? = null
)