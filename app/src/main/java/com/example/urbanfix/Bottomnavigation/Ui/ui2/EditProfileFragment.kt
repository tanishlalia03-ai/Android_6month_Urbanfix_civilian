package com.example.urbanfix.Bottomnavigation.Ui.ui2

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
import com.example.urbanfix.Appwrite.AppwriteManager
import com.example.urbanfix.R
import com.example.urbanfix.databinding.FragmentEditprofileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditprofileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("Users")
    private val bucketID = "6996dc680036b04ee5f0"
    private val projectID = "6996dc3e00250d7ae563"
    private val appwriteManager by lazy { AppwriteManager.getInstance(requireContext().applicationContext) }

    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivProfilePicture.setImageURI(it)
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
            if (snapshot.exists()) {
                binding.apply {
                    etEditName.setText(snapshot.child("name").value?.toString())
                    etEditEmail.setText(snapshot.child("email").value?.toString())
                    etEditMobile.setText(snapshot.child("phone").value?.toString())

                    val img = snapshot.child("imageUrl").value?.toString()
                    if (!img.isNullOrEmpty()) {
                        Glide.with(this@EditProfileFragment)
                            .load(img)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .into(ivProfilePicture)
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndSave() {
        val name = binding.etEditName.text.toString().trim()
        val email = binding.etEditEmail.text.toString().trim()
        val phone = binding.etEditMobile.text.toString().trim()
        val uid = auth.currentUser?.uid ?: return

        if (name.isEmpty()) {
            binding.etEditName.error = "Required"; return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEditEmail.error = "Invalid Email"; return
        }
        if (phone.length != 10) {
            binding.etEditMobile.error = "Invalid Phone"; return
        }

        // UI Feedback: Disable button and show user something is happening
        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.text = "Updating..."

        lifecycleScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "phone" to phone
                )

                // Only upload if a new image was selected
                if (selectedImageUri != null) {
                    val file = uriToFile(selectedImageUri!!)
                    if (file != null) {
                        val result = appwriteManager.uploadImage(bucketID, file)
                        val newUrl = "https://fra.cloud.appwrite.io/v1/storage/buckets/$bucketID/files/${result.id}/view?project=$projectID"
                        updates["imageUrl"] = newUrl
                    }
                }

                // Update Realtime Database
                database.child(uid).updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }.addOnFailureListener { e ->
                    resetButton(e.message)
                }

            } catch (e: Exception) {
                Log.e("UrbanFixError", "Error: ${e.message}")
                resetButton(e.message)
            }
        }
    }

    private fun resetButton(error: String?) {
        binding.btnSaveProfile.isEnabled = true
        binding.btnSaveProfile.text = "SAVE CHANGES"
        Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
    }

    private suspend fun uriToFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "update_img_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
            file
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}