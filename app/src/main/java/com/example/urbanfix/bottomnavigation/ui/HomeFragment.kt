package com.example.urbanfix.bottomnavigation.ui

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var tvUserName: TextView? = null
    private var tvPending: TextView? = null
    private var tvProgress: TextView? = null
    private var tvCompleted: TextView? = null
    private var pieChart: PieChart? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize Views
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvPending = view.findViewById(R.id.tv_count_pending)
        tvProgress = view.findViewById(R.id.tv_count_progress)
        tvCompleted = view.findViewById(R.id.tv_count_completed)
        pieChart = view.findViewById(R.id.complaintsPieChart)

        // 2. Load User Data
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            fetchUserName(currentUserId)
            listenForComplaintSummary(currentUserId)
        }
    }

    private fun fetchUserName(uid: String) {
        db.getReference("Users").child(uid).child("name").get().addOnSuccessListener { snapshot ->
            if (isAdded) {
                val nameFromDb = snapshot.getValue(String::class.java)
                tvUserName?.text = nameFromDb ?: "User"
            }
        }
    }

    private fun listenForComplaintSummary(uid: String) {
        // Efficient Query: Only fetch complaints for the current user
        val userComplaintsQuery = db.getReference("Complaints")
            .orderByChild("civilianId")
            .equalTo(uid)

        userComplaintsQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                var pCount = 0
                var prCount = 0
                var cCount = 0

                for (ds in snapshot.children) {
                    val status = ds.child("status").getValue(Int::class.java) ?: -1
                    when (status) {
                        0 -> pCount++
                        1 -> prCount++
                        2 -> cCount++
                    }
                }

                // Format with leading zeros (e.g., 05 instead of 5)
                tvPending?.text = String.format("%02d", pCount)
                tvProgress?.text = String.format("%02d", prCount)
                tvCompleted?.text = String.format("%02d", cCount)

                updatePieChart(pCount, prCount, cCount)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updatePieChart(p: Int, pr: Int, c: Int) {
        val chart = pieChart ?: return
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // Use the professional tonal colors
        if (p > 0) {
            entries.add(PieEntry(p.toFloat(), "Pending"))
            colors.add(Color.parseColor("#D32F2F"))
        }
        if (pr > 0) {
            entries.add(PieEntry(pr.toFloat(), "Active"))
            colors.add(Color.parseColor("#388E3C"))
        }
        if (c > 0) {
            entries.add(PieEntry(c.toFloat(), "Resolved"))
            colors.add(Color.parseColor("#0097A7"))
        }

        if (entries.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Start reporting to see data!")
            chart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 13f
            valueTextColor = if (isDarkMode()) Color.WHITE else Color.BLACK
            sliceSpace = 3f

            // Labels and Connecting Lines (Your preferred style)
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.4f
            valueLineWidth = 2f
            valueLineColor = if (isDarkMode()) Color.WHITE else Color.BLACK
        }

        val pieData = PieData(dataSet)
        chart.data = pieData

        chart.apply {
            description.isEnabled = false

            // Padding to prevent labels from cutting off
            setExtraOffsets(25f, 10f, 25f, 10f)

            // Modern Donut Look
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)

            centerText = "Overview"
            setCenterTextColor(if (isDarkMode()) Color.WHITE else Color.BLACK)
            setCenterTextSize(16f)

            // Slice Label Styling
            setEntryLabelColor(if (isDarkMode()) Color.WHITE else Color.BLACK)
            setEntryLabelTextSize(12f)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                textColor = if (isDarkMode()) Color.WHITE else Color.BLACK
                yOffset = 5f
            }

            animateY(1000)
            invalidate()
        }
    }

    private fun isDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up view references to prevent memory leaks on your HP laptop
        tvUserName = null
        tvPending = null
        tvProgress = null
        tvCompleted = null
        pieChart = null
    }
}