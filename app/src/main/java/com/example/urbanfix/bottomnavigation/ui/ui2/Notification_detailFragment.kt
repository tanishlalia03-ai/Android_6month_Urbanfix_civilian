package com.example.urbanfix.bottomnavigation.ui.ui2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.urbanfix.databinding.FragmentNotificationDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class Notification_detailFragment : Fragment() {

    private var _binding: FragmentNotificationDetailBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Retrieve data with safe defaults
        val title = arguments?.getString("title") ?: "No Title"
        val body = arguments?.getString("body") ?: "No message content available."
        val timestamp = arguments?.getLong("timestamp") ?: 0L

        // 2. Map data to your XML views
        binding.tvDetailTitle.text = title
        binding.tvDetailBody.text = body

        // 3. Format date professionally (e.g., 25 May 2024, 02:30 PM)
        if (timestamp != 0L) {
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            binding.tvDetailTime.text = sdf.format(Date(timestamp))
        } else {
            binding.tvDetailTime.visibility = View.GONE
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Crucial to avoid memory leaks
        _binding = null
    }
}