package com.example.urbanfix

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.urbanfix.Bottomnavigation.BottomLayoutActivity
import com.example.urbanfix.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email == "tanishlalia@gmail.com" && password == "123") {
                val intent = Intent(this, BottomLayoutActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Incorrect Credentials", Toast.LENGTH_SHORT).show()
            }
        }


        binding.btnSignUp.setOnClickListener {
               val intent = Intent(this, SignupActivity2::class.java)
                startActivity(intent)
        }
    }
}