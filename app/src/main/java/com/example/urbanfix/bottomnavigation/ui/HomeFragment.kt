package com.example.urbanfix.bottomnavigation.ui

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.urbanfix.R
import com.example.urbanfix.firebase.UserModel // Ensure this import is correct
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. Fetch and Set User Name ---
        val tvUserName = view.findViewById<TextView>(R.id.tv_user_name)
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            dbRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // This maps the Firebase data directly to your UserModel class
                    val user = snapshot.getValue(UserModel::class.java)
                    if (user != null && !user.name.isNullOrEmpty()) {
                        tvUserName.text = user.name
                    }
                }
            }.addOnFailureListener {
                tvUserName.text = "User"
            }
        }

        // --- 2. PieChart Setup (Kept exactly as your original) ---
        val pieChart = view.findViewById<PieChart>(R.id.complaintsPieChart)

        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(5f, "Pending"))
        entries.add(PieEntry(3f, "Progress"))
        entries.add(PieEntry(12f, "Completed"))

        val colors = arrayListOf(
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#00BCD4")  // Cyan
        )

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.apply {
            description.isEnabled = false
            centerText = "Summary"
            setCenterTextSize(16f)
            setHoleRadius(50f)
            animateY(1200)
            invalidate()
        }
    }
}