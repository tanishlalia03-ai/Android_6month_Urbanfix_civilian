package com.example.urbanfix

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.urbanfix.appwrite.AppwriteManager
import com.example.urbanfix.databinding.ActivitySignup2Binding
import com.example.urbanfix.firebase.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class SignupActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySignup2Binding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Appwrite Configuration
    private val bucketID = "6996dc680036b04ee5f0"
    private val appwriteManager by lazy { AppwriteManager.getInstance(applicationContext) }

    private lateinit var progressDialog: ProgressDialog

    private var selectedImageUri: Uri? = null
    private var selectedIdUri: Uri? = null

    // Picker for Profile Picture
    private val profilePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivProfile.setImageURI(it)
            binding.ivProfile.imageTintList = null
        }
    }

    // Picker for ID Proof
    private val idProofPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedIdUri = it
            // Visual feedback that ID is selected
            Toast.makeText(this, "ID Proof Document Selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignup2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this).apply {
            setTitle("Creating Account")
            setMessage("Uploading files and saving data...")
            setCancelable(false)
        }

        // Listeners for Image Selection
        binding.cvProfileImage.setOnClickListener {
            profilePicker.launch("image/*")
        }

        // Make sure you have a button or view in XML with ID: btnSelectIdProof
        binding.btnSelectIdProof.setOnClickListener {
            idProofPicker.launch("image/*")
        }

        binding.btnSignUpSubmit.setOnClickListener {
            validateAndRegister()
        }
    }

    private fun validateAndRegister() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmailAddress.text.toString().trim()
        val mobile = binding.etMobileNumber.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val role = binding.etRole.text.toString().trim()
        val pass = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()

        when {
            selectedImageUri == null -> Toast.makeText(this, "Please select a profile photo", Toast.LENGTH_SHORT).show()
            selectedIdUri == null -> Toast.makeText(this, "Please upload an ID Proof", Toast.LENGTH_SHORT).show()
            name.isEmpty() -> binding.tilFullName.error = "Name required"
            !email.contains("@") -> binding.tilEmail.error = "Invalid email"
            mobile.length != 10 -> Toast.makeText(this, "Invalid Phone (10 digits)", Toast.LENGTH_SHORT).show()
            pass.length < 6 -> binding.tilPassword.error = "Min 6 chars"
            pass != confirmPass -> binding.tilConfirmPassword.error = "Passwords do not match"
            !binding.cbTerms.isChecked -> Toast.makeText(this, "Please accept terms", Toast.LENGTH_SHORT).show()
            else -> {
                progressDialog.show()
                registerInAuth(email, pass, name, mobile, address, role)
            }
        }
    }

    private fun registerInAuth(email: String, pass: String, name: String, phone: String, address: String, role: String) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: ""
                lifecycleScope.launch {
                    handleFileUploadsAndFirebase(uid, name, email, phone, address, role)
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun handleFileUploadsAndFirebase(uid: String, name: String, email: String, phone: String, address: String, role: String) {
        try {
            // STEP 1: Upload Profile Image
            val imageUrl = appwriteManager.uploadAndGetUrl(this, bucketID, selectedImageUri!!)

            // STEP 2: Upload ID Proof
            val idUrl = appwriteManager.uploadAndGetUrl(this, bucketID, selectedIdUri!!)

            if (imageUrl == null || idUrl == null) {
                cleanup("Failed to upload one or more documents.")
                return
            }

            // STEP 3: Get FCM Token and Save to Firebase
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                val user = UserModel(
                    name, email, phone, address, uid,
                    token, role, System.currentTimeMillis(), imageUrl, idUrl
                )

                database.child("Users").child(uid).setValue(user).addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { cleanup(it.message) }
            }.addOnFailureListener { cleanup("FCM Token generation failed") }

        } catch (e: Exception) {
            Log.e("SignupError", "Process failed", e)
            cleanup(e.message)
        }
    }

    private fun cleanup(error: String?) {
        // If anything fails after Auth creation, delete the Auth user to allow a retry
        auth.currentUser?.delete()?.addOnCompleteListener {
            progressDialog.dismiss()
            Toast.makeText(this, "Failed: $error", Toast.LENGTH_LONG).show()
        }
    }
}