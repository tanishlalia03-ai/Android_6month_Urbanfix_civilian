package com.example.urbanfix.bottomnavigation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R
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
    private val notificationList = mutableListOf<Pair<String, NotificationModel>>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(context)
        adapter = NotificationAdapter(notificationList)
        binding.recyclerViewNotifications.adapter = adapter

        binding.shimmerViewContainer.startShimmer()
        fetchNotifications()
    }

    private fun fetchNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database = FirebaseDatabase.getInstance().getReference("CivilianMessages")

        // Use addValueEventListener to keep the list synced
        database.orderByChild("civilianId").equalTo(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Safety check for fragment lifecycle

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
                    binding.recyclerViewNotifications.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()

                    binding.tvNoNotifications.visibility = if (notificationList.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding != null) {
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.visibility = View.GONE
                    }
                }
            })
    }

    inner class NotificationAdapter(private val list: List<Pair<String, NotificationModel>>) :
        RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

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

            // --- BLUE DOT LOGIC ---
            // If read is true, hide dot. If read is false, show dot.
            holder.itemBinding.viewUnreadDot.visibility = if (item.read) View.GONE else View.VISIBLE

            holder.itemView.setOnClickListener { view ->
                // 1. Mark as read in Firebase safely
                if (!item.read) {
                    FirebaseDatabase.getInstance().getReference("CivilianMessages")
                        .child(firebaseId)
                        .child("read")
                        .setValue(true)
                }

                // 2. Pass data to Detail Fragment via Bundle
                val bundle = Bundle().apply {
                    putString("title", item.title)
                    putString("body", item.body)
                    putLong("timestamp", item.timestamp)
                }

                // 3. Use the correct ACTION ID from your nav_graph
                try {
                    view.findNavController().navigate(
                        R.id.action_notifications_to_viewDetail,
                        bundle
                    )
                } catch (e: Exception) {
                    // This prevents the crash if the ID is wrong, and lets you see the error in Logcat
                    e.printStackTrace()
                }
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}