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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class ComplaintAdapter(
    private var complaintList: MutableList<ComplaintModel>
) : RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder>() {

    private val TAG = "UrbanFix_Adapter"
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Complaints")

    class ComplaintViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // ID and Button are removed to match the new WhatsApp style
        val titleText: TextView = view.findViewById(R.id.tvComplaintTitle)
        val descText: TextView = view.findViewById(R.id.tvComplaintDescription)
        val statusText: TextView = view.findViewById(R.id.tvStatus)
        val dateText: TextView = view.findViewById(R.id.tvDateTime)
        val ivFavorite: CheckBox = view.findViewById(R.id.ivStar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_complaints, parent, false)
        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        val item = complaintList[position]

        // 1. Content Binding
        holder.titleText.text = item.title ?: "Untitled Issue"
        holder.descText.text = item.description ?: "No description provided."
        holder.dateText.text = formatTimestamp(item.timeStamp)

        // 2. Status Color Logic
        when (item.status ?: 0) {
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
            else -> {
                holder.statusText.text = "Unknown"
                holder.statusText.setTextColor(Color.GRAY)
            }
        }

        // 3. Favorite Toggle Logic
        holder.ivFavorite.setOnCheckedChangeListener(null)
        holder.ivFavorite.isChecked = item.validation ?: false

        holder.ivFavorite.setOnCheckedChangeListener { _, isChecked ->
            val uid = auth.currentUser?.uid
            val complaintId = item.complaintId

            if (uid != null && complaintId != null) {
                database.child(uid).child(complaintId).child("validation").setValue(isChecked)
                    .addOnSuccessListener { Log.d(TAG, "Favorite status updated") }
            }
        }

        // 4. THE BIG CHANGE: Full Row Click Navigation
        holder.itemView.setOnClickListener { view ->
            item.complaintId?.let { id ->
                val bundle = Bundle().apply {
                    putString("complaintId", id)
                }
                // This replaces the button click with a full card click
                Navigation.findNavController(view).navigate(R.id.action_complaints_to_viewDetail, bundle)
            }
        }
    }

    override fun getItemCount(): Int = complaintList.size

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return ""
        return try {
            // "hh:mm a" gives you that clean "12:30 PM" WhatsApp look
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
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