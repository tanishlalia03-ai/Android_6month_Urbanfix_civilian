package com.example.urbanfix.Bottomnavigation.Ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R
import com.example.urbanfix.Recyclerview.Complaint
import com.example.urbanfix.Recyclerview.complaint_adapter

class ComplaintsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_complaints, container, false)

        val fakeData = listOf(
            Complaint("UF-101", "Pending"),
            Complaint("UF-102", "In Progress"),
            Complaint("UF-103", "Resolved"),
            Complaint("UF-104", "Under Review"),
            Complaint("UF-105", "Fixed")
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvComplaints)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = complaint_adapter(fakeData)

        return view
    }
}