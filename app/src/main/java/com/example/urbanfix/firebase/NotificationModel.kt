package com.example.urbanfix.firebase

data class NotificationModel(
    val title: String = "",
    val body: String = "",
    val civilianId: String = "",
    val timestamp: Long = 0L,
    val complaintId: String = "",
    val read: Boolean = false
)