package com.example.urbanfix.bottomnavigation.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.urbanfix.firebase.UserModel
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Users")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentProfileBinding.bind(view)

        // Load data from Firebase & Appwrite
        loadUserData()

        // 1. Navigate to Edit Profile Fragment
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        }

        // 2. Logout Logic
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        database.child(uid).get().addOnSuccessListener { snapshot ->
            // Safety check: ensure fragment is still attached to activity
            if (!isAdded) return@addOnSuccessListener

            val user = snapshot.getValue(UserModel::class.java)

            if (user != null) {
                binding.tvProfileName.text = user.name ?: "No Name"
                binding.tvProfileEmail.text = user.email ?: "No Email"
                binding.tvProfilePhone.text = user.phone ?: "No Phone"

                // FIX: Check if imageUrl is valid and use a cleaner Glide call
                val imagePath = user.imageUrl
                if (!imagePath.isNullOrEmpty()) {
                    Glide.with(requireContext()) // Use requireContext() for better stability
                        .load(imagePath)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_apple) // This shows if the URL is broken or unauthorized
                        .circleCrop()
                        .into(binding.ivProfileDisplay)
                } else {
                    // If no image, set default placeholder
                    binding.ivProfileDisplay.setImageResource(R.drawable.ic_person)
                }
            }
        }.addOnFailureListener { e ->
            if (isAdded) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}