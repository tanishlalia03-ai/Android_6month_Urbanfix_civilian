package com.example.urbanfix.Firebase

data class UserModel(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val uid: String? = null,
    val deviceToken: String? = null,
    val role: String? = null,
    val time: Long? = null,
    val imageUrl: String? = null
)