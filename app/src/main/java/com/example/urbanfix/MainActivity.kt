package com.example.urbanfix

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils // Add this import
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.urbanfix.bottomnavigation.BottomLayoutActivity
import com.example.urbanfix.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the shake animation from resources
        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        if (auth.currentUser != null) {
            startActivity(Intent(this, BottomLayoutActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, BottomLayoutActivity::class.java))
                            finish()
                        } else {
                            // Trigger shake on Firebase failure
                            binding.btnLogin.startAnimation(shakeAnimation)
                            val errorMsg = task.exception?.message
                            Toast.makeText(this, "Login Failed: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                // Trigger shake on empty field validation
                binding.btnLogin.startAnimation(shakeAnimation)

                if (email.isEmpty()) binding.emailInputLayout.error = "Enter email"
                if (password.isEmpty()) binding.passwordInputLayout.error = "Enter password"
            }
        }

        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity2::class.java))
        }
    }
}

