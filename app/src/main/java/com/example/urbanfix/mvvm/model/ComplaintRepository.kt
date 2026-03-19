package com.example.urbanfix.mvvm.model

import com.example.urbanfix.firebase.ComplaintModel
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ComplaintRepository {
    private val database = FirebaseDatabase.getInstance().getReference("Complaints")

    fun getLiveComplaints(): Flow<List<ComplaintModel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // This converts the Firebase data into your list of ComplaintModels
                val items = snapshot.children.mapNotNull { it.getValue(ComplaintModel::class.java) }
                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        database.addValueEventListener(listener)

        // Clean up the listener when not in use to save your HP laptop's resources
        awaitClose { database.removeEventListener(listener) }
    }
}