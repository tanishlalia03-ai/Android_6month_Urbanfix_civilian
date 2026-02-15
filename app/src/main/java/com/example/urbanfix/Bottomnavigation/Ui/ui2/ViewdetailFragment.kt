package com.example.urbanfix.Bottomnavigation.Ui.ui2

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

class ViewdetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_viewdetail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find the PieChart by the ID we added to the XML
        val pieChart = view.findViewById<PieChart>(R.id.complaintPieChart)

        // 2. Prepare Fake Data (e.g., 75% Work Done, 25% Pending)
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(75f, "Fixed"))
        entries.add(PieEntry(25f, "Pending"))

        // 3. Style the Dataset
        val dataSet = PieDataSet(entries, "")

        // Using professional colors: Blue for progress, Light Gray for pending
        val colors = arrayListOf(
            Color.parseColor("#2196F3"), // Primary Blue
            Color.parseColor("#E0E0E0")  // Light Gray
        )
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        // 4. Configure the Chart Look
        val pieData = PieData(dataSet)
        pieChart.data = pieData

        // UI Enhancements
        pieChart.description.isEnabled = false // Hide small description label
        pieChart.isDrawHoleEnabled = true      // Make it a donut chart
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.centerText = "75% Done"       // Text in the middle
        pieChart.setCenterTextColor(Color.parseColor("#2196F3"))
        pieChart.setCenterTextSize(16f)

        // 5. Animation (This will wow your mentor!)
        pieChart.animateY(1400)

        pieChart.invalidate() // Refresh chart
    }
}