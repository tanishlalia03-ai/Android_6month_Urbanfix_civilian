package com.example.urbanfix.bottomnavigation.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.urbanfix.MainActivity
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentProfileBinding
import com.example.urbanfix.firebase.UserModel
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
        binding.btnLogoutMini.setOnClickListener {
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

            // 1. Sign out from Firebase
            auth.signOut()

            // 2. Clear the Task Stack and redirect to Login
            // Replace LoginActivity::class.java with your actual Login Activity class
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // 3. Finish the host activity (usually the BottomNav activity)
            requireActivity().finish()

            Toast.makeText(requireContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show()
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
            // Safety check for Fragment lifecycle
            if (_binding == null || !isAdded) return@addOnSuccessListener

            val user = snapshot.getValue(UserModel::class.java)

            // Update UI using the user object
            binding.apply {
                tvHeaderName.text = user?.name ?: "User Name"
                tvProfileEmail.text = user?.email ?: "No Email Provided"
                tvProfilePhone.text = user?.phone ?: "No Phone Provided"
                tvProfileRole.text = user?.role?.uppercase() ?: "USER"
                tvProfileAddress.text = user?.address ?: "No Address Added"

                // Load Profile Image with Glide
                if (!user?.imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(user?.imageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
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