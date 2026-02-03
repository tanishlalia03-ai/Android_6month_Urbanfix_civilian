package com.example.urbanfix.Bottomnavigation

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.urbanfix.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomLayoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bottom_layout)

        // 1. Find the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment

        // 2. Get the NavController
        val navController = navHostFragment.navController

        // 3. Find the BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 4. Link them together
        bottomNavigationView.setupWithNavController(navController)
    }
}