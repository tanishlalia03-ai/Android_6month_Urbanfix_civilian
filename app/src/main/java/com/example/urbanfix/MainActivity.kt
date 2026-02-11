package com.example.urbanfix

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.urbanfix.Bottomnavigation.BottomLayoutActivity
import com.example.urbanfix.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 1. Initialize Firebase Auth
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Direct Login: Skip login page if user is already signed in
        if (auth.currentUser != null) {
            startActivity(Intent(this, BottomLayoutActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Reset Errors
            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // 3. Real Firebase Authentication Login
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Success! Navigate to the Dashboard
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, BottomLayoutActivity::class.java)
                            startActivity(intent)
                            finish() // Prevent going back to login with back button
                        } else {
                            // Failed! Show the actual reason (wrong password, no user, etc.)
                            val errorMsg = task.exception?.message
                            Toast.makeText(this, "Login Failed: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                if (email.isEmpty()) binding.emailInputLayout.error = "Enter email"
                if (password.isEmpty()) binding.passwordInputLayout.error = "Enter password"
            }
        }

        binding.btnSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity2::class.java)
            startActivity(intent)
        }
    }
}