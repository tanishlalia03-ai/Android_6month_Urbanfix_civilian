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
import com.example.urbanfix.Recyclerview.ComplaintAdapter
import com.google.android.material.chip.ChipGroup

class ComplaintsFragment : Fragment() {

    private var adapter: ComplaintAdapter? = null
    private val displayedList = mutableListOf<Complaint>()

    // MOVE THIS HERE: This prevents the list from resetting when you switch tabs
    private val fullList: MutableList<Complaint> by lazy {
        mutableListOf(
            Complaint("#12345", "Pending", isFavorite = true),
            Complaint("#12346", "Active", isFavorite = false),
            Complaint("#12347", "Completed", isFavorite = true),
            Complaint("#12348", "Pending", isFavorite = false),
            Complaint("#12349", "Active", isFavorite = true)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_complaints, container, false)

        // Sync the displayed list with our permanent fullList
        displayedList.clear()
        displayedList.addAll(fullList)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvComplaints)
        val filterChipGroup = view.findViewById<ChipGroup>(R.id.filterChipGroup)

        // Pass a callback to the adapter to update fullList in real-time
        adapter = ComplaintAdapter(displayedList) { updatedItem ->
            val index = fullList.indexOfFirst { it.id == updatedItem.id }
            if (index != -1) {
                fullList[index].isFavorite = updatedItem.isFavorite
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        filterChipGroup?.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedId = checkedIds.firstOrNull() ?: R.id.chip_all
            filterData(selectedId)
        }

        return view
    }

    private fun filterData(chipId: Int) {
        val filtered = when (chipId) {
            R.id.chip_favorite -> fullList.filter { it.isFavorite }
            R.id.chip_pending -> fullList.filter { it.status == "Pending" }
            R.id.chip_completed -> fullList.filter { it.status == "Completed" }
            R.id.chip_active -> fullList.filter { it.status == "Active" }
            else -> fullList
        }

        displayedList.clear()
        displayedList.addAll(filtered)
        adapter?.notifyDataSetChanged()
    }
}