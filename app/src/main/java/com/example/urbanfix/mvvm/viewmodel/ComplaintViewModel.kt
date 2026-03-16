package com.example.urbanfix.mvvm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urbanfix.firebase.ComplaintModel
import com.example.urbanfix.mvvm.model.ComplaintRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComplaintViewModel : ViewModel() {

    // Create an instance of the Repository from your model package
    private val repository = ComplaintRepository()

    // Internal mutable state that we update (Private to keep data safe)
    private val _complaints = MutableStateFlow<List<ComplaintModel>>(emptyList())

    // External read-only state that the Fragment (View) observes
    val complaints: StateFlow<List<ComplaintModel>> = _complaints.asStateFlow()

    init {
        // Automatically start listening to Firebase when the app opens this page
        fetchComplaints()
    }

    private fun fetchComplaints() {
        viewModelScope.launch {
            try {
                // Collect the real-time Flow from the Repository
                repository.getLiveComplaints().collect { list ->
                    _complaints.value = list
                }
            } catch (e: Exception) {
                // If there's a Firebase error, it won't crash the app
                e.printStackTrace()
            }
        }
    }
}