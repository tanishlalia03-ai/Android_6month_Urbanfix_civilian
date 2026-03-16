package com.example.urbanfix.recyclerview

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R
import com.example.urbanfix.firebase.ComplaintModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class ComplaintAdapter(
    private var complaintList: MutableList<ComplaintModel>
) : RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder>() {

    private val TAG = "UrbanFix_Adapter"
    private val database = FirebaseDatabase.getInstance().getReference("Complaints")

    class ComplaintViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idText: TextView = view.findViewById(R.id.tvComplaintId)
        val statusText: TextView = view.findViewById(R.id.tvStatus)
        val dateText: TextView = view.findViewById(R.id.tvDateTime)
        val btnViewDetail: MaterialButton = view.findViewById(R.id.btnViewDetail)
        val ivFavorite: CheckBox = view.findViewById(R.id.ivStar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_complaints, parent, false)
        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        val item = complaintList[position]

        holder.idText.text = "ID: ${item.complaintId?.takeLast(5) ?: "N/A"}"
        holder.dateText.text = formatTimestamp(item.timeStamp)

        // Status UI Logic
        when (item.status) {
            0 -> {
                holder.statusText.text = "Pending"
                holder.statusText.setTextColor(Color.parseColor("#D32F2F"))
            }
            1 -> {
                holder.statusText.text = "In Progress"
                holder.statusText.setTextColor(Color.parseColor("#1976D2"))
            }
            2 -> {
                holder.statusText.text = "Completed"
                holder.statusText.setTextColor(Color.parseColor("#388E3C"))
            }
        }

        // --- FAVORITE SYNC LOGIC ---
        // 1. Remove listener before setting state to avoid loops
        holder.ivFavorite.setOnCheckedChangeListener(null)
        holder.ivFavorite.isChecked = item.validation ?: false

        // 2. Set new listener to update Firebase
        holder.ivFavorite.setOnCheckedChangeListener { _, isChecked ->
            item.complaintId?.let { id ->
                database.child(id).child("validation").setValue(isChecked)
                    .addOnSuccessListener {
                        Log.d(TAG, "Favorite status updated to $isChecked for $id")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Failed to update favorite status")
                    }
            }
        }

        holder.btnViewDetail.setOnClickListener { view ->
            val bundle = Bundle().apply { putString("complaintId", item.complaintId) }
            Navigation.findNavController(view).navigate(R.id.action_complaints_to_viewDetail, bundle)
        }
    }

    override fun getItemCount(): Int = complaintList.size

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "N/A"
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun updateList(newList: List<ComplaintModel>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = complaintList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                complaintList[oldPos].complaintId == newList[newPos].complaintId
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                complaintList[oldPos] == newList[newPos]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        complaintList.clear()
        complaintList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}