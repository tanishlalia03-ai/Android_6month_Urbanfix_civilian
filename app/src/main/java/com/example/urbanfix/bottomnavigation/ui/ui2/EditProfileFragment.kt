package com.example.urbanfix.bottomnavigation.ui.ui2

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.urbanfix.R
import com.example.urbanfix.appwrite.AppwriteManager
import com.example.urbanfix.databinding.FragmentEditprofileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditprofileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Users")

    // Appwrite Details
    private val bucketID = "6996dc680036b04ee5f0"
    private val appwriteManager by lazy { AppwriteManager.getInstance(requireContext().applicationContext) }

    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            Glide.with(requireContext())
                .load(it)
                .circleCrop()
                .into(binding.ivEditProfilePicture)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchExistingData()

        // Image Picker
        binding.btnChangeImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // --- NEW: Password Toggle Logic ---
        binding.btnShowPasswordFields.setOnClickListener {
            if (binding.passwordLayout.visibility == View.GONE) {
                binding.passwordLayout.visibility = View.VISIBLE
                binding.btnShowPasswordFields.text = "Keep current password"
            } else {
                binding.passwordLayout.visibility = View.GONE
                binding.btnShowPasswordFields.text = "Change Password?"
                binding.etNewPassword.text?.clear()
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            validateAndSave()
        }
    }

    private fun fetchExistingData() {
        val uid = auth.currentUser?.uid ?: return
        database.child(uid).get().addOnSuccessListener { snapshot ->
            if (_binding == null || !isAdded) return@addOnSuccessListener

            if (snapshot.exists()) {
                binding.apply {
                    etEditName.setText(snapshot.child("name").value?.toString() ?: "")
                    etEditEmail.setText(snapshot.child("email").value?.toString() ?: "")
                    etEditMobile.setText(snapshot.child("phone").value?.toString() ?: "")
                    etEditAddress.setText(snapshot.child("address").value?.toString() ?: "")

                    // Display role as read-only
                    val role = snapshot.child("role").value?.toString()?.uppercase(Locale.getDefault()) ?: "USER"
                    // Note: If etEditRole doesn't exist in your new card-based XML, remove this line
                    // etEditRole.setText(role)

                    val img = snapshot.child("imageUrl").value?.toString()
                    if (!img.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(img)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivEditProfilePicture)
                    }
                }
            }
        }
    }

    private fun validateAndSave() {
        val name = binding.etEditName.text.toString().trim()
        val phone = binding.etEditMobile.text.toString().trim()
        val address = binding.etEditAddress.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val user = auth.currentUser
        val uid = user?.uid ?: return

        // Basic Validation
        if (name.isEmpty()) { binding.etEditName.error = "Required"; return }
        if (phone.length < 10) { binding.etEditMobile.error = "Invalid phone"; return }
        if (address.isEmpty()) { binding.etEditAddress.error = "Address required"; return }

        // Password Validation (only if visible)
        if (binding.passwordLayout.visibility == View.VISIBLE) {
            if (newPassword.isEmpty() || newPassword.length < 6) {
                binding.etNewPassword.error = "Password must be 6+ chars"
                return
            }
        }

        // Start Update UI
        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Updating..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "phone" to phone,
                    "address" to address
                )

                // 1. Upload Image if changed
                if (selectedImageUri != null) {
                    val newUrl = appwriteManager.uploadAndGetUrl(requireContext(), bucketID, selectedImageUri!!)
                    if (newUrl != null) updates["imageUrl"] = newUrl
                }

                // 2. Update Firebase Auth Password (if requested)
                if (binding.passwordLayout.visibility == View.VISIBLE) {
                    user.updatePassword(newPassword).addOnFailureListener {
                        Log.e("UrbanFix", "Auth Update Failed: ${it.message}")
                    }
                }

                // 3. Update Realtime Database
                database.child(uid).updateChildren(updates).addOnSuccessListener {
                    if (isAdded && _binding != null) {
                        Toast.makeText(requireContext(), "Profile Updated!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }.addOnFailureListener { e ->
                    lifecycleScope.launch(Dispatchers.Main) { resetButton(e.message) }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { resetButton(e.message) }
            }
        }
    }

    private fun resetButton(error: String?) {
        if (!isAdded || _binding == null) return
        binding.btnSaveProfile.isEnabled = true
        binding.btnSaveProfile.text = "SAVE CHANGES"
        Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}