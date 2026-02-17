package com.example.urbanfix.Recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R
import com.google.android.material.button.MaterialButton

class ComplaintAdapter(
    private val complaintList: MutableList<Complaint>,
    private val onFavoriteChanged: (Complaint) -> Unit
) : RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder>() {

    class ComplaintViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idText: TextView = view.findViewById(R.id.tvComplaintId)
        val statusText: TextView = view.findViewById(R.id.tvStatus)
        val btnViewDetail: MaterialButton = view.findViewById(R.id.btnViewDetail)
        val ivFavorite: CheckBox = view.findViewById(R.id.ivStar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_complaints, parent, false)
        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        val item = complaintList[position]

        holder.idText.text = item.id
        holder.statusText.text = item.status

        // Stop listener, set state, then restart listener to prevent logical loops
        holder.ivFavorite.setOnCheckedChangeListener(null)
        holder.ivFavorite.isChecked = item.isFavorite

        holder.ivFavorite.setOnClickListener {
            val isChecked = (it as CheckBox).isChecked
            item.isFavorite = isChecked
            onFavoriteChanged(item) // Real-time update to Fragment
        }

        holder.btnViewDetail.setOnClickListener { view ->
            Navigation.findNavController(view).navigate(R.id.action_complaints_to_viewDetail)
        }
    }

    override fun getItemCount() = complaintList.size
}