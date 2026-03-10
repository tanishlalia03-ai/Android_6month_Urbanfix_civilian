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
import com.example.urbanfix.databinding.FragmentEditprofileBinding
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
                .into(binding.ivProfilePicture)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchExistingData()

        binding.btnEditImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.btnSaveProfile.setOnClickListener {
            validateAndSave()
        }
    }

    private fun fetchExistingData() {
        val uid = auth.currentUser?.uid ?: return
        database.child(uid).get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener

            if (snapshot.exists()) {
                binding.apply {
                    etEditName.setText(snapshot.child("name").value?.toString() ?: "")
                    etEditEmail.setText(snapshot.child("email").value?.toString() ?: "")
                    etEditMobile.setText(snapshot.child("phone").value?.toString() ?: "")

                    val img = snapshot.child("imageUrl").value?.toString()
                    if (!img.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(img)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivProfilePicture)
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
        val uid = auth.currentUser?.uid ?: return

        if (name.isEmpty()) { binding.etEditName.error = "Required"; return }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { binding.etEditEmail.error = "Invalid Email"; return }
        if (phone.length != 10) { binding.etEditMobile.error = "10 digits required"; return }

        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Updating..."

        lifecycleScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "phone" to phone
                )

                // PROFESSIONAL ONE-LINE UPLOAD
                if (selectedImageUri != null) {
                    val newImageUrl = appwriteManager.uploadAndGetUrl(requireContext(), bucketID, selectedImageUri!!)
                    if (newImageUrl != null) {
                        updates["imageUrl"] = newImageUrl
                    } else {
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show()
                        // Decide if you want to stop here or continue updating text data
                    }
                }

                // Push to Firebase
                database.child(uid).updateChildren(updates).addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
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
        if (!isAdded) return
        binding.btnSaveProfile.isEnabled = true
        binding.btnSaveProfile.text = "SAVE CHANGES"
        Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}