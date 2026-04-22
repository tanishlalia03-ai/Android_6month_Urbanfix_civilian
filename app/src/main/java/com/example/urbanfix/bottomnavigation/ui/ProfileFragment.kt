package com.example.urbanfix.bottomnavigation.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
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

        loadUserData()

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        }

        binding.btnLogoutMini.setOnClickListener {
            showLogoutBottomSheet()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        binding.profileProgressBar.visibility = View.VISIBLE
        binding.profileScrollView.visibility = View.INVISIBLE

        database.child(uid).get().addOnSuccessListener { snapshot ->
            if (_binding == null || !isAdded) return@addOnSuccessListener

            val user = snapshot.getValue(UserModel::class.java)

            binding.apply {
                tvHeaderName.text = user?.name ?: "User Name"
                tvProfileEmail.text = user?.email ?: "No Email Provided"
                tvProfilePhone.text = user?.phone ?: "No Phone Provided"
                tvProfileRole.text = user?.role?.uppercase() ?: "USER"
                tvProfileAddress.text = user?.address ?: "No Address Added"

                if (!user?.imageUrl.isNullOrEmpty()) {
                    // 1. Load the small circular image
                    Glide.with(this@ProfileFragment)
                        .load(user?.imageUrl)
                        .placeholder(R.drawable.ic_person)
                        .circleCrop()
                        .into(ivProfileDisplay)

                    // 2. Add click listener to show full image
                    ivProfileDisplay.setOnClickListener {
                        showFullImageDialog(user?.imageUrl!!)
                    }
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

    // --- NEW FUNCTION: Show Full Screen Image Dialog ---
    private fun showFullImageDialog(imageUrl: String) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_full_image_view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        val fullImageView = dialog.findViewById<ImageView>(R.id.ivFullDisplay)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseFullImage)

        // Load image into the dialog
        Glide.with(requireContext())
            .load(imageUrl)
            .placeholder(R.drawable.ic_person)
            .into(fullImageView)

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showLogoutBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.layout_logout_bottom_sheet, null, false)

        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_logout)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_logout)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            auth.signOut()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
            Toast.makeText(requireContext(), "Logged Out Successfully", Toast.LENGTH_SHORT).show()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.setContentView(view)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}