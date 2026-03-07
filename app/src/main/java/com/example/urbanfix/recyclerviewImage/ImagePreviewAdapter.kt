package com.example.urbanfix.recyclerviewImage

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R

class ImagePreviewAdapter(private val imageList: ArrayList<Uri>) :
    RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    // ViewHolder holds the references to the views for each individual item
    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivPreview)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val currentUri = imageList[position]

        // Load the image into the ImageView
        holder.ivPreview.setImageURI(currentUri)

        // Handle the "X" (ic_cancel) button click
        holder.btnRemove.setOnClickListener {
            val actualPosition = holder.bindingAdapterPosition
            if (actualPosition != RecyclerView.NO_POSITION) {
                // 1. Remove from the data list
                imageList.removeAt(actualPosition)

                // 2. Notify the adapter to animate the removal
                notifyItemRemoved(actualPosition)

                // 3. Update the rest of the list indices
                notifyItemRangeChanged(actualPosition, imageList.size)
            }
        }
    }

    override fun getItemCount(): Int = imageList.size
}