package com.example.urbanfix.mvvm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urbanfix.firebase.ComplaintModel
import com.example.urbanfix.mvvm.model.ComplaintRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ComplaintViewModel : ViewModel() {

    private val repository = ComplaintRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _complaints = MutableStateFlow<List<ComplaintModel>>(emptyList())
    val complaints: StateFlow<List<ComplaintModel>> = _complaints.asStateFlow()

    init {
        fetchComplaints()
    }

    private fun fetchComplaints() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            viewModelScope.launch {
                try {
                    // We pass the UID to the repository here
                    repository.getLiveComplaints(currentUserId).collect { list ->
                        _complaints.value = list
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}