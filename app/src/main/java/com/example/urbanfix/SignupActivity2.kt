package com.example.urbanfix

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.urbanfix.databinding.ActivitySignup2Binding

class SignupActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivitySignup2Binding

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

            // 1. Reset all errors to null first
            binding.tilFullName.error = null
            binding.tilEmail.error = null
            binding.tilMobile.error = null
            binding.tilPassword.error = null
            binding.tilConfirmPassword.error = null


            when {
                name.isEmpty() -> binding.tilFullName.error = "Name required"
                !email.contains("@") || !email.contains(".") -> binding.tilEmail.error = "Invalid email"
                mobile.length != 10 -> binding.tilMobile.error = "Must be 10 digits"
                pass.isEmpty() -> binding.tilPassword.error = "Password required"
                pass != confirmPass -> binding.tilConfirmPassword.error = "Passwords match failed"
                !binding.cbTerms.isChecked -> Toast.makeText(this, "Accept Terms first", Toast.LENGTH_SHORT).show()
                else -> {
                    // Success!
                    Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}