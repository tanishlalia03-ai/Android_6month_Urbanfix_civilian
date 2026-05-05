package com.example.urbanfix.bottomnavigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentNotificationsBinding
import com.example.urbanfix.databinding.ItemNotificationBinding
import com.example.urbanfix.firebase.NotificationModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val notificationList = mutableListOf<Pair<String, NotificationModel>>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(context)

        // Pass the long click logic to the adapter
        adapter = NotificationAdapter(notificationList) { firebaseId ->
            showDeleteDialog(firebaseId)
        }
        binding.recyclerViewNotifications.adapter = adapter

        binding.shimmerViewContainer.startShimmer()
        fetchNotifications()
    }

    private fun fetchNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("CivilianMessages")

        database.orderByChild("civilianId").equalTo(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    notificationList.clear()
                    for (data in snapshot.children) {
                        val notification = data.getValue(NotificationModel::class.java)
                        val key = data.key
                        if (notification != null && key != null) {
                            notificationList.add(Pair(key, notification))
                        }
                    }
                    notificationList.sortByDescending { it.second.timestamp }

                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE

                    if (notificationList.isEmpty()) {
                        binding.recyclerViewNotifications.visibility = View.GONE
                        binding.layoutEmptyNotifications.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmptyNotifications.visibility = View.GONE
                        binding.recyclerViewNotifications.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding != null) {
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.visibility = View.GONE
                    }
                }
            })
    }

    private fun showDeleteDialog(firebaseId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Delete") { _, _ ->
                deleteNotification(firebaseId)
            }
            .show()
    }

    private fun deleteNotification(firebaseId: String) {
        FirebaseDatabase.getInstance().getReference("CivilianMessages")
            .child(firebaseId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show()
            }
    }

    inner class NotificationAdapter(
        private val list: List<Pair<String, NotificationModel>>,
        private val onLongClick: (String) -> Unit // New parameter for long click
    ) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemNotificationBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val (firebaseId, item) = list[position]

            holder.itemBinding.tvNotificationTitle.text = item.title
            holder.itemBinding.tvNotificationBody.text = item.body

            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.itemBinding.tvNotificationTime.text = sdf.format(Date(item.timestamp))

            holder.itemBinding.viewUnreadDot.visibility = if (item.read) View.GONE else View.VISIBLE

            // Normal Click to view details
            holder.itemView.setOnClickListener { view ->
                if (!item.read) {
                    FirebaseDatabase.getInstance().getReference("CivilianMessages")
                        .child(firebaseId)
                        .child("read")
                        .setValue(true)
                }

                val bundle = Bundle().apply {
                    putString("title", item.title)
                    putString("body", item.body)
                    putLong("timestamp", item.timestamp)
                }

                try {
                    view.findNavController().navigate(
                        R.id.action_notifications_to_viewDetail,
                        bundle
                    )
                } catch (e: Exception) { e.printStackTrace() }
            }

            // LONG CLICK to trigger delete
            holder.itemView.setOnLongClickListener {
                onLongClick(firebaseId)
                true
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}