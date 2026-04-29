package com.example.urbanfix.bottomnavigation.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.urbanfix.R
import com.example.urbanfix.firebase.ComplaintModel
import com.example.urbanfix.recyclerview.ComplaintAdapter
import com.example.urbanfix.mvvm.viewmodel.ComplaintViewModel
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ComplaintsFragment : Fragment(R.layout.fragment_complaints) {

    private val viewModel: ComplaintViewModel by viewModels()
    private var adapter: ComplaintAdapter? = null
    private var fullList = listOf<ComplaintModel>()

    // UI Components
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoData: TextView
    private lateinit var recyclerView: RecyclerView

    private var currentFilterId: Int = R.id.chip_all

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        recyclerView = view.findViewById(R.id.rvComplaints)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoData = view.findViewById(R.id.tvNoData)
        val filterChipGroup = view.findViewById<ChipGroup>(R.id.filterChipGroup)

        // Set up Adapter - initializing with an empty list
        adapter = ComplaintAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Initial Loading State
        showLoading(true)

        // Observe Data with Flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.complaints.collect { newList ->
                    // Guard against null or issues during data transition
                    fullList = newList ?: emptyList()

                    showLoading(false)
                    // Re-apply the current filter whenever data changes
                    filterData(currentFilterId)
                }
            }
        }

        // Chip selection logic
        filterChipGroup?.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterId = checkedIds.firstOrNull() ?: R.id.chip_all
            filterData(currentFilterId)
        }
    }

    private fun filterData(chipId: Int) {
        val filtered = when (chipId) {
            R.id.chip_pending -> fullList.filter { it.status == 0 }
            R.id.chip_active -> fullList.filter { it.status == 1 }
            R.id.chip_completed -> fullList.filter { it.status == 2 }
            R.id.chip_favorite -> fullList.filter { it.validation == true }
            else -> fullList
        }

        // Update UI based on filtered list size
        if (filtered.isEmpty()) {
            tvNoData.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoData.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            // Safely update the adapter
            adapter?.updateList(filtered)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tvNoData.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE

        }
    }
}