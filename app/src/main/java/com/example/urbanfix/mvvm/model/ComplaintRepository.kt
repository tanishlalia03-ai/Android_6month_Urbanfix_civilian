package com.example.urbanfix.mvvm.model

import android.util.Log
import com.example.urbanfix.firebase.ComplaintModel
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ComplaintRepository {
    private val rootRef = FirebaseDatabase.getInstance().getReference("Complaints")

    fun getLiveComplaints(userId: String): Flow<List<ComplaintModel>> = callbackFlow {
        // Pointing to: Complaints -> Specific User UID
        val userComplaintsRef = rootRef.child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull {
                    try {
                        // This prevents the crash if data format is wrong
                        it.getValue(ComplaintModel::class.java)
                    } catch (e: Exception) {
                        Log.e("ComplaintRepo", "Mapping Error: ${e.message}")
                        null
                    }
                }
                trySend(items)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        userComplaintsRef.addValueEventListener(listener)

        awaitClose { userComplaintsRef.removeEventListener(listener) }
    }
}