package com.example.urbanfix.bottomnavigation.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Users")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        // Load data from Firebase
        loadUserData()

        // 1. Navigate to Edit Profile Fragment
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        }

        // 2. Logout Logic
        binding.btnLogout.setOnClickListener {
            showLogoutBottomSheet()
        }
    }

    private fun showLogoutBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_logout_bottom_sheet, null, false)

        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_logout)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_logout)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            auth.signOut()
            Toast.makeText(requireContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show()

            // Close the current activity and return to Login
            requireActivity().finish()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        // Show loading state
        binding.profileProgressBar.visibility = View.VISIBLE
        binding.profileScrollView.visibility = View.INVISIBLE

        database.child(uid).get().addOnSuccessListener { snapshot ->
            // Safety check: verify fragment is still attached to UI before updating views
            if (_binding == null || !isAdded) return@addOnSuccessListener

            // map snapshot to your UserModel
            val name = snapshot.child("name").getValue(String::class.java)
            val email = snapshot.child("email").getValue(String::class.java)
            val phone = snapshot.child("phone").getValue(String::class.java)
            val role = snapshot.child("role").getValue(String::class.java)
            val address = snapshot.child("address").getValue(String::class.java)
            val imagePath = snapshot.child("imageUrl").getValue(String::class.java)

            // Update UI with Firebase Data
            binding.apply {
                tvHeaderName.text = name ?: "User Name"
                tvProfileEmail.text = email ?: "No Email Provided"
                tvProfilePhone.text = phone ?: "No Phone Provided"

                // New Fields from the updated XML
                tvProfileRole.text = role?.uppercase() ?: "USER"
                tvProfileAddress.text = address ?: "No Address Added"

                // Load Profile Image
                if (!imagePath.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(imagePath)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivProfileDisplay)
                }
            }

            // Hide loading and show content
            binding.profileProgressBar.visibility = View.GONE
            binding.profileScrollView.visibility = View.VISIBLE

        }.addOnFailureListener { e ->
            if (_binding != null && isAdded) {
                binding.profileProgressBar.visibility = View.GONE
                binding.profileScrollView.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }
}