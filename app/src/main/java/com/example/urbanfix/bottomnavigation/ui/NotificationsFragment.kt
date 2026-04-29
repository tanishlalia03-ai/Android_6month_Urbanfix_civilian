package com.example.urbanfix.bottomnavigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.databinding.FragmentNotificationsBinding
import com.example.urbanfix.databinding.ItemNotificationBinding
import com.example.urbanfix.firebase.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private val notificationList = mutableListOf<NotificationModel>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(notificationList)
        binding.recyclerViewNotifications.adapter = adapter

        // Start Shimmer Effect immediately
        binding.shimmerViewContainer.startShimmer()

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("CivilianMessages")

        database.orderByChild("civilianId").equalTo(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notificationList.clear()
                    for (data in snapshot.children) {
                        val notification = data.getValue(NotificationModel::class.java)
                        if (notification != null) {
                            notificationList.add(notification)
                        }
                    }
                    notificationList.sortByDescending { it.timestamp }

                    // --- SHIMMER LOGIC START ---
                    // Stop shimmer once data arrives
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE

                    // Show RecyclerView and update adapter
                    binding.recyclerViewNotifications.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()

                    // Check if empty
                    if (notificationList.isEmpty()) {
                        binding.tvNoNotifications.visibility = View.VISIBLE
                    } else {
                        binding.tvNoNotifications.visibility = View.GONE
                    }
                    // --- SHIMMER LOGIC END ---
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                }
            })
    }

    inner class NotificationAdapter(private val list: List<NotificationModel>) :
        RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ItemNotificationBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.itemBinding.tvNotificationTitle.text = item.title
            holder.itemBinding.tvNotificationBody.text = item.body

            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.itemBinding.tvNotificationTime.text = sdf.format(Date(item.timestamp))
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop shimmer to save memory when fragment is destroyed
        binding.shimmerViewContainer.stopShimmer()
        _binding = null
    }
} 