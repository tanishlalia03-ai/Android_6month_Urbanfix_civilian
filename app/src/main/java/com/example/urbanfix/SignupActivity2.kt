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
import com.example.urbanfix.Appwrite.AppwriteManager
import com.example.urbanfix.Firebase.UserModel
import com.example.urbanfix.databinding.ActivitySignup2Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SignupActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySignup2Binding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    // Appwrite Details
    private val bucketID = "6996dc680036b04ee5f0"
    private val appwriteManager by lazy { AppwriteManager.getInstance(applicationContext) }

    private lateinit var progressDialog: ProgressDialog
    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.ivProfile.setImageURI(it)
            binding.ivProfile.imageTintList = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignup2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this).apply {
            setTitle("Creating Account")
            setMessage("Uploading Image & Data...")
            setCancelable(false)
        }

        binding.cvProfileImage.setOnClickListener {
            imagePicker.launch("image/*")
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
            selectedImageUri == null -> Toast.makeText(this, "Select a photo", Toast.LENGTH_SHORT).show()
            name.isEmpty() -> binding.tilFullName.error = "Name required"
            !email.contains("@") -> binding.tilEmail.error = "Invalid email"
            mobile.length != 10 -> Toast.makeText(this, "Invalid Phone", Toast.LENGTH_SHORT).show()
            pass.length < 6 -> binding.tilPassword.error = "Min 6 chars"
            pass != confirmPass -> binding.tilConfirmPassword.error = "Mismatch"
            !binding.cbTerms.isChecked -> Toast.makeText(this, "Accept Terms", Toast.LENGTH_SHORT).show()
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
                // Start the Appwrite Upload inside a Coroutine
                lifecycleScope.launch {
                    handleAppwriteUploadAndFirebaseData(uid, name, email, phone, address, role)
                }
            } else {
                progressDialog.dismiss()
                Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun handleAppwriteUploadAndFirebaseData(uid: String, name: String, email: String, phone: String, address: String, role: String) {
        try {
            // 1. Convert Uri to File for Appwrite
            val file = uriToFile(selectedImageUri!!)
            if (file == null) {
                cleanup("Could not process image file")
                return
            }

            // 2. Upload to Appwrite Storage
            val appwriteResult = appwriteManager.uploadImage(bucketID, file)

            // 3. Generate the viewable URL for the UserModel
            val imageUrl = "https://fra.cloud.appwrite.io/v1/storage/buckets/$bucketID/files/${appwriteResult.id}/view?project=6996dc3e00250d7ae563"

            // 4. Get FCM Token and Save to Firebase Realtime Database
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                val user = UserModel(name, email, phone, address, uid, token, role, System.currentTimeMillis(), imageUrl)

                database.child("Users").child(uid).setValue(user).addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { cleanup(it.message) }
            }.addOnFailureListener { cleanup("FCM Token failed") }

        } catch (e: Exception) {
            Log.e("SignupError", "Appwrite Upload Failed", e)
            cleanup(e.message)
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanup(error: String?) {
        auth.currentUser?.delete()?.addOnCompleteListener {
            progressDialog.dismiss()
            Toast.makeText(this, "Failed: $error", Toast.LENGTH_LONG).show()
        }
    }
}