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

        // Load data from Firebase
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

        // --- ADDED VISIBILITY LOGIC START ---
        binding.profileProgressBar.visibility = View.VISIBLE
        binding.profileScrollView.visibility = View.INVISIBLE
        // --- ADDED VISIBILITY LOGIC END ---

        database.child(uid).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener

            val user = snapshot.getValue(UserModel::class.java)

            if (user != null) {
                binding.tvProfileName.text = user.name ?: "No Name"
                binding.tvProfileEmail.text = user.email ?: "No Email"
                binding.tvProfilePhone.text = user.phone ?: "No Phone"

                val imagePath = user.imageUrl
                if (!imagePath.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(imagePath)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(binding.ivProfileDisplay)
                } else {
                    binding.ivProfileDisplay.setImageResource(R.drawable.ic_person)
                }
            }

            // --- HIDE PROGRESS BAR & SHOW CONTENT ---
            binding.profileProgressBar.visibility = View.GONE
            binding.profileScrollView.visibility = View.VISIBLE

        }.addOnFailureListener { e ->
            if (isAdded) {
                // --- HIDE PROGRESS BAR ON ERROR ---
                binding.profileProgressBar.visibility = View.GONE
                binding.profileScrollView.visibility = View.VISIBLE

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}