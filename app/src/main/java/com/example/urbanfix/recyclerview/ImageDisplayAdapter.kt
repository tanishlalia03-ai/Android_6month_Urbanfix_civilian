package com.example.urbanfix.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.urbanfix.R

class ImageDisplayAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImageDisplayAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivDetailImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view_detail_image, parent, false) // Updated name
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(imageUrls[position])
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = imageUrls.size
}