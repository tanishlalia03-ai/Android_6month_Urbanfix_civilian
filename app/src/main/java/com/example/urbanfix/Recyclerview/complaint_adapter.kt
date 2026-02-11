package com.example.urbanfix.Recyclerview


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R

class complaint_adapter(private val complaintList: List<Complaint>) :
    RecyclerView.Adapter<complaint_adapter.ComplaintViewHolder>() {

    class ComplaintViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idText: TextView = view.findViewById(R.id.tvComplaintId)
        val statusText: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_complaints, parent, false)
        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        val item = complaintList[position]
        holder.idText.text = "Complaint ID: ${item.id}"
        holder.statusText.text = item.status
    }

    override fun getItemCount() = complaintList.size
}