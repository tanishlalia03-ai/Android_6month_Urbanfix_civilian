package com.example.urbanfix

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.urbanfix.databinding.ActivitySignup2Binding
import com.google.firebase.auth.FirebaseAuth

class SignupActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySignup2Binding

    // 1. Initialize Firebase Auth
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignup2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUpSubmit.setOnClickListener {
            // Get user input
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmailAddress.text.toString().trim()
            val mobile = binding.etMobileNumber.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // Reset all errors
            binding.tilFullName.error = null
            binding.tilEmail.error = null
            binding.tilMobile.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null

            when {
                name.isEmpty() -> binding.tilFullName.error = "Name required"
                !email.contains("@") || !email.contains(".") -> binding.tilEmail.error = "Invalid email"
                mobile.length != 10 -> binding.tilMobile.error = "Must be 10 digits"
                pass.length < 6 -> binding.tilPassword.error = "Password must be at least 6 characters"
                pass != confirmPass -> binding.tilConfirmPassword.error = "Passwords match failed"
                !binding.cbTerms.isChecked -> Toast.makeText(this, "Accept Terms first", Toast.LENGTH_SHORT).show()
                else -> {
                    // 2. Implementation of Authentication
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Sign up success
                                Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                                finish() // Closes the signup screen
                            } else {
                                // If sign up fails, display a message to the user.
                                val errorMessage = task.exception?.message
                                Toast.makeText(this, "Auth Failed: $errorMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        }
    }
}