package com.example.urbanfix.Bottomnavigation.Ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.urbanfix.Firebase.UserModel
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
            val user = snapshot.getValue(UserModel::class.java)

            if (user != null) {
                binding.tvProfileName.text = user.name ?: "No Name"
                binding.tvProfileEmail.text = user.email ?: "No Email"
                binding.tvProfilePhone.text = user.phone ?: "No Phone"

                if (!user.imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(user.imageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_apple)
                        .circleCrop()
                        .into(binding.ivProfileDisplay)
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}