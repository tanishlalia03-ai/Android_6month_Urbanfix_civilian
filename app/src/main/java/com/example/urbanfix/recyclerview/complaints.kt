package com.example.urbanfix.recyclerview

data class Complaint(
    val id: String,
    val status: String,
    var isFavorite: Boolean = false
)