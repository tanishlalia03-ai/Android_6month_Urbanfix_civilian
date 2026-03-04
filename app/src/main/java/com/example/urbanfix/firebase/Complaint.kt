package com.example.urbanfix.firebase


data class Complaint(
    val id: String?= null,
    val name: String?= null,
    val time: Long?= null,
    val imageUrl :String?= null,
    val status : String?= null,
    val loaction: String?= null
    )
