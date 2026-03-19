package com.example.urbanfix.bottomnavigation.ui.ui2

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.urbanfix.appwrite.AppwriteManager
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentEditprofileBinding // Ensure this matches your XML filename
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

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
                .into(binding.ivEditProfilePicture) // Updated ID to match our previous XML
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchExistingData()

        binding.btnChangeImage.setOnClickListener {
            imagePicker.launch("image/*")
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

                    // NEW: Load Address and Role
                    etEditAddress.setText(snapshot.child("address").value?.toString() ?: "")
                    etEditRole.setText(snapshot.child("role").value?.toString()?.uppercase() ?: "USER")

                    val img = snapshot.child("imageUrl").value?.toString()
                    if (!img.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(img)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivEditProfilePicture)
                    }
                }
            }
        }.addOnFailureListener {
            if (isAdded) Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndSave() {
        val name = binding.etEditName.text.toString().trim()
        val email = binding.etEditEmail.text.toString().trim()
        val phone = binding.etEditMobile.text.toString().trim()
        val address = binding.etEditAddress.text.toString().trim() // NEW
        val uid = auth.currentUser?.uid ?: return

        // Validation
        if (name.isEmpty()) { binding.etEditName.error = "Required"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEditEmail.error = "Invalid Email"; return }
        if (phone.length != 10) { binding.etEditMobile.error = "10 digits required"; return }
        if (address.isEmpty()) { binding.etEditAddress.error = "Address required"; return }

        // UI State
        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Updating..."

        lifecycleScope.launch {
            try {
                // Prepare Updates Map
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "address" to address // NEW
                )

                // Handle Image Upload with Appwrite
                if (selectedImageUri != null) {
                    val newImageUrl = appwriteManager.uploadAndGetUrl(requireContext(), bucketID, selectedImageUri!!)
                    if (newImageUrl != null) {
                        updates["imageUrl"] = newImageUrl
                    } else {
                        Log.e("UrbanFix", "Image upload returned null URL")
                    }
                }

                // Update Firebase Database
                database.child(uid).updateChildren(updates).addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }.addOnFailureListener { e ->
                    resetButton(e.message)
                }

            } catch (e: Exception) {
                Log.e("UrbanFixError", "Save Error: ${e.message}")
                resetButton(e.message)
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