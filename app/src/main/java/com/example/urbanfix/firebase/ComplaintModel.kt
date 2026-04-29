package com.example.urbanfix.firebase

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class ComplaintModel(
    val complaintId: String? = null,
    val location: String? = null,
    val title: String? = null,
    val description: String? = null,
    val issueType: String? = null,
    val images: ArrayList<String>? = ArrayList(),
    val civilianId: String? = null,
    val allottedOfficerId: String? = null,
    val departmentId: String? = null,

    // This allows your old code to work even if the DB uses lowercase 'timestamp'
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timeStamp: Long? = null,

    val status: Int? = 0,
    val validation: Boolean? = null,
    val priority: Int? = 0,
    var latitude: Double? = 0.0,
    var longitude: Double? = 0.0,

    // New Fields from the Admin integration
    val phone: Long? = null,
    val etaHours: Int? = 0,
    val estimatedResolutionAt: Long? = 0L,
    val etaUpdatedAt: Long? = 0L,
    val etaReason: String? = null,
    val complaintKey: String? = null
) {
    // Firebase required empty constructor
    constructor() : this(
        null, null, null, null, null, ArrayList(),
        null, null, null, null, 0, null, 0, 0.0, 0.0,
        null, 0, 0L, 0L, null, null
    )
}