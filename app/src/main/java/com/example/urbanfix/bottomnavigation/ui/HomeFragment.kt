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

    private lateinit var tvUserName: TextView
    private lateinit var tvPending: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvCompleted: TextView
    private lateinit var pieChart: PieChart

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

        // 2. Fetch Data
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            fetchUserName(currentUserId)
            listenForComplaintSummary(currentUserId)
        }
    }

    private fun fetchUserName(uid: String) {
        db.getReference("Users").child(uid).child("name").get().addOnSuccessListener { snapshot ->
            val nameFromDb = snapshot.getValue(String::class.java)
            tvUserName.text = nameFromDb ?: "User"
        }.addOnFailureListener {
            tvUserName.text = "User"
        }
    }

    private fun listenForComplaintSummary(uid: String) {
        val complaintsRef = db.getReference("Complaints")

        complaintsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var pCount = 0
                var prCount = 0
                var cCount = 0

                for (ds in snapshot.children) {
                    val complaintCivilianId = ds.child("civilianId").getValue(String::class.java)
                    val status = ds.child("status").getValue(Int::class.java) ?: -1

                    if (complaintCivilianId == uid) {
                        when (status) {
                            0 -> pCount++
                            1 -> prCount++
                            2 -> cCount++
                        }
                    }
                }

                tvPending.text = String.format("%02d", pCount)
                tvProgress.text = String.format("%02d", prCount)
                tvCompleted.text = String.format("%02d", cCount)

                updatePieChart(pCount, prCount, cCount)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updatePieChart(p: Int, pr: Int, c: Int) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        if (p > 0) {
            entries.add(PieEntry(p.toFloat(), "Pending"))
            colors.add(Color.parseColor("#F44336"))
        }
        if (pr > 0) {
            entries.add(PieEntry(pr.toFloat(), "Progress"))
            colors.add(Color.parseColor("#4CAF50"))
        }
        if (c > 0) {
            entries.add(PieEntry(c.toFloat(), "Completed"))
            colors.add(Color.parseColor("#00BCD4"))
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No complaints reported yet")
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 13f
        dataSet.valueTextColor = Color.WHITE

        // Move labels outside if slices are small to prevent overlap
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLineColor = if (isDarkMode()) Color.WHITE else Color.BLACK

        val pieData = PieData(dataSet)
        pieChart.data = pieData

        pieChart.apply {
            description.isEnabled = false
            setExtraOffsets(25f, 5f, 25f, 5f) // Padding for outside labels

            // --- Legend Styling (Fixes overlap below chart) ---
            legend.apply {
                isEnabled = true
                isWordWrapEnabled = true
                xEntrySpace = 20f
                yEntrySpace = 8f
                formToTextSpace = 6f
                form = Legend.LegendForm.CIRCLE
                textColor = if (isDarkMode()) Color.WHITE else Color.BLACK
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            // --- Center Text Styling ---
            centerText = "Status"
            setCenterTextColor(if (isDarkMode()) Color.WHITE else Color.BLACK)
            setCenterTextSize(16f)
            setHoleColor(Color.TRANSPARENT)

            setEntryLabelColor(if (isDarkMode()) Color.WHITE else Color.BLACK)
            setEntryLabelTextSize(12f)

            animateY(800)
            invalidate()
        }
    }

    private fun isDarkMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}