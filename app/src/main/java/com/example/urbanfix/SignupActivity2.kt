package com.example.urbanfix

import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
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

        binding.cvProfileImage.setOnClickListener {
            profilePicker.launch("image/*")
        }

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
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.tilEmail.error = "Invalid email"
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
                val user = auth.currentUser
                val uid = user?.uid ?: ""

                // Send Verification Email
                user?.sendEmailVerification()?.addOnCompleteListener { vTask ->
                    if (vTask.isSuccessful) {
                        Log.d("Signup", "Verification email sent.")
                    }
                }

                // Proceed with file uploads and data saving
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
            // STEP 1: Upload to Appwrite
            val imageUrl = appwriteManager.uploadAndGetUrl(this, bucketID, selectedImageUri!!)
            val idUrl = appwriteManager.uploadAndGetUrl(this, bucketID, selectedIdUri!!)

            if (imageUrl == null || idUrl == null) {
                cleanup("Failed to upload documents to Appwrite.")
                return
            }

            // STEP 2: Get FCM Token and Save to Realtime Database
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                val user = UserModel(
                    name, email, phone, address, uid,
                    token, role, System.currentTimeMillis(), imageUrl, idUrl
                )

                database.child("Users").child(uid).setValue(user).addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Success! Check your email to verify account.", Toast.LENGTH_LONG).show()

                    // Crucial: Sign out so they must login with verification check
                    auth.signOut()
                    finish()
                }.addOnFailureListener { cleanup(it.message) }
            }.addOnFailureListener { cleanup("FCM Token generation failed") }

        } catch (e: Exception) {
            Log.e("SignupError", "Process failed", e)
            cleanup(e.message)
        }
    }

    private fun cleanup(error: String?) {
        auth.currentUser?.delete()?.addOnCompleteListener {
            progressDialog.dismiss()
            Toast.makeText(this, "Failed: $error", Toast.LENGTH_LONG).show()
        }
    }
}