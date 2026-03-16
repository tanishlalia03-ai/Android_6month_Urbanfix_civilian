package com.example.urbanfix.firebase

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ComplaintModel(
    val complaintId: String? = null,
    val location: String? = null,
    val title: String? = null,
    val description: String? = null,
    val issueType: String ?= null, //dropdown
    val images: ArrayList<String> ?= ArrayList(),
    val civilianId: String? = null,
    val allottedOfficerId: String? = null,   // ""
    val departmentId: String? = null,  //
    val timeStamp: Long? = null,
    val status: Int? = 0,  // 0 = pending,1 = progress,2 = competed ,initial = 0
    val validation: Boolean? = null, // false
    val priority: Int? = 0,  // initially = 0, 0 = low, 1= medium, 2= high
    var latitude: Double ?= 0.0,
    var longitude: Double ?= 0.0

){
    // ADD THIS: Explicit empty constructor for Firebase
    constructor() : this(
        null, null, null, null, null, ArrayList(),
        null, null, null, null, 0, null, 0, 0.0, 0.0
    )
}