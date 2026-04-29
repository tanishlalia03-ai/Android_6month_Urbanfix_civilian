package com.example.urbanfix

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AnimationUtils
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

        val shakeAnimation = AnimationUtils.loadAnimation(this, R.anim.shake)

        // 1. Auto-login check (With Loading State)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Show Progress, Hide Login UI
            binding.loginGroup.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE

            currentUser.reload().addOnCompleteListener { task ->
                if (currentUser.isEmailVerified) {
                    navigateToHome()
                } else {
                    // Not verified: Show Login UI again and sign out
                    binding.loginGroup.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                    auth.signOut()
                    Toast.makeText(this, "Please verify your email to continue", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 2. Login Button Logic
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                binding.btnLogin.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE // Show progress during login attempt

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        binding.btnLogin.isEnabled = true
                        binding.progressBar.visibility = View.GONE

                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                                navigateToHome()
                            } else {
                                // User exists but not verified
                                auth.signOut()
                                binding.btnLogin.startAnimation(shakeAnimation)
                                Toast.makeText(this, "Please verify your email first. Check your inbox.", Toast.LENGTH_LONG).show()

                                showResendVerificationDialog(user)
                            }
                        } else {
                            binding.btnLogin.startAnimation(shakeAnimation)
                            val errorMsg = task.exception?.message ?: "Login failed"
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                binding.btnLogin.startAnimation(shakeAnimation)
            }
        }

        // 3. Forgot Password Logic
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Please enter your email first"
                binding.etEmail.requestFocus()
                binding.tvForgotPassword.startAnimation(shakeAnimation)
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailInputLayout.error = "Please enter a valid email address"
            } else {
                binding.emailInputLayout.error = null
                sendPasswordReset(email)
            }
        }

        // 4. Navigate to Signup
        binding.btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity2::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Invalid email format"
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }
        return isValid
    }

    private fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showResendVerificationDialog(user: com.google.firebase.auth.FirebaseUser?) {
        user?.sendEmailVerification()?.addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "A new verification link has been sent.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, BottomLayoutActivity::class.java))
        finish()
    }
}