package com.example.urbanfix.Bottomnavigation.Ui

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.urbanfix.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the chart using the ID from your XML screenshot
        val pieChart = view.findViewById<PieChart>(R.id.complaintsPieChart)

        // 1. Set up entries matching your summary card numbers
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(5f, "Pending"))
        entries.add(PieEntry(3f, "Progress"))
        entries.add(PieEntry(12f, "Completed"))

        // 2. Set colors to match your UI (Red, Green, Cyan)
        val colors = arrayListOf(
            Color.parseColor("#F44336"), // Red for Pending
            Color.parseColor("#4CAF50"), // Green for Progress
            Color.parseColor("#00BCD4")  // Cyan for Completed
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // 3. Final Chart Setup
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.apply {
            description.isEnabled = false
            centerText = "Summary"
            setCenterTextSize(16f)
            setHoleRadius(50f) // Makes it look like a nice ring
            animateY(1200)     // Smooth entry animation
            invalidate()       // Refresh the view
        }
    }
}