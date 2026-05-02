package com.example.urbanfix.bottomnavigation.ui.ui2

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentViewdetailBinding // Ensure this matches your layout name
import com.example.urbanfix.firebase.ComplaintModel
import com.example.urbanfix.recyclerview.ImageDisplayAdapter
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class ViewdetailFragment : Fragment(R.layout.fragment_viewdetail) {

    private var _binding: FragmentViewdetailBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentViewdetailBinding.bind(view)

        val complaintId = arguments?.getString("complaintId")
        val currentUserId = auth.currentUser?.uid

        if (complaintId != null && currentUserId != null) {
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("Complaints")
                .child(currentUserId)
                .child(complaintId)

            dbRef.get().addOnSuccessListener { snapshot ->
                binding.detailsProgressBar.visibility = View.GONE

                if (snapshot.exists()) {
                    val model = snapshot.getValue(ComplaintModel::class.java)
                    model?.let { data ->
                        // Bind UI data using binding object
                        binding.tvComplaintId.text = "Complaint Id: #${data.complaintId?.takeLast(5) ?: "N/A"}"
                        binding.tvCategory.text = "Category: ${data.issueType ?: "General"}"
                        binding.tvDescription.text = data.description ?: "No description provided."
                        binding.tvLocation.text = "Location: ${data.location ?: "Unknown"}"
                        binding.tvDate.text = "Date: ${formatDate(data.timeStamp)}"

                        val priorityText = when(data.priority ?: 0) {
                            2 -> "High"
                            1 -> "Medium"
                            else -> "Low"
                        }
                        binding.tvUrgency.text = "Urgency: $priorityText"

                        val statusText = when(data.status ?: 0) {
                            0 -> "Pending"
                            1 -> "In Progress"
                            2 -> "Completed"
                            else -> "Unknown"
                        }
                        binding.tvStatus.text = "Status: $statusText"

                        // Image Slider Logic
                        if (!data.images.isNullOrEmpty()) {
                            val adapter = ImageDisplayAdapter(data.images!!)
                            binding.viewPagerImages.adapter = adapter

                            if (data.images!!.size > 1) {
                                binding.tabIndicator.visibility = View.VISIBLE
                                TabLayoutMediator(binding.tabIndicator, binding.viewPagerImages) { _, _ -> }.attach()
                            } else {
                                binding.tabIndicator.visibility = View.GONE
                            }
                        }
                        setupPieChart(binding.complaintPieChart, data.status ?: 0)
                    }
                } else {
                    Toast.makeText(context, "Complaint no longer exists", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                binding.detailsProgressBar.visibility = View.GONE
                Toast.makeText(context, "Error fetching details", Toast.LENGTH_SHORT).show()
            }

            binding.btnDeleteComplaint.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Withdraw Complaint")
                    .setMessage("Are you sure you want to delete this complaint?")
                    .setPositiveButton("Yes, Delete") { _, _ ->
                        binding.detailsProgressBar.visibility = View.VISIBLE
                        dbRef.removeValue().addOnSuccessListener {
                            binding.detailsProgressBar.visibility = View.GONE
                            Toast.makeText(context, "Successfully Withdrawn", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }.addOnFailureListener {
                            binding.detailsProgressBar.visibility = View.GONE
                            Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

        } else {
            binding.detailsProgressBar.visibility = View.GONE
            Toast.makeText(context, "Authorization or ID error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "N/A"
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun setupPieChart(pieChart: PieChart, status: Int) {
        val entries = ArrayList<PieEntry>()
        val progressValue = when (status) {
            2 -> 100f
            1 -> 50f
            else -> 10f
        }
        entries.add(PieEntry(progressValue, "Done"))
        entries.add(PieEntry(100f - progressValue, "Remaining"))

        val dataSet = PieDataSet(entries, "")
        val colorCode = when(status) {
            2 -> "#4CAF50"
            1 -> "#2196F3"
            else -> "#F44336"
        }

        dataSet.colors = arrayListOf(Color.parseColor(colorCode), Color.parseColor("#E0E0E0"))
        dataSet.setDrawValues(false)
        pieChart.data = PieData(dataSet)
        pieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            centerText = "${progressValue.toInt()}%"
            setCenterTextColor(Color.parseColor(colorCode))
            setCenterTextSize(18f)
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}