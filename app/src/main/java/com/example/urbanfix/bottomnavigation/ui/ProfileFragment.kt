package com.example.urbanfix.bottomnavigation.ui

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

        // 2. Logout Logic (Updated ID to match the new mini button in header)
        binding.btnLogoutMini.setOnClickListener {
            showLogoutBottomSheet()
        }
    }

    private fun showLogoutBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        // Ensure this layout exists in your res/layout folder
        val view = layoutInflater.inflate(R.layout.layout_logout_bottom_sheet, null, false)

        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_logout)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_logout)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            auth.signOut()
            Toast.makeText(requireContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to Login Activity
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
        // Note: Using alpha or invisible keeps the layout structure while loading
        binding.profileScrollView.visibility = View.INVISIBLE

        database.child(uid).get().addOnSuccessListener { snapshot ->
            // Safety check for Fragment lifecycle
            if (_binding == null || !isAdded) return@addOnSuccessListener

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

                // Using the individual card TextViews from the new XML
                tvProfileRole.text = role?.uppercase() ?: "USER"
                tvProfileAddress.text = address ?: "No Address Added"

                // Load Profile Image with Glide
                if (!imagePath.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(imagePath)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop() // Modern circular look
                        .into(ivProfileDisplay)
                }
            }

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
        _binding = null
    }
}