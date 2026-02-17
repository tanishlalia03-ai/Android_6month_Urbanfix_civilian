package com.example.urbanfix.Bottomnavigation

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.urbanfix.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BottomLayoutActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // --- 1. THEME MODE LOGIC (Dark/Light) ---
        val savedThemeIndex = sharedPref.getInt("theme_mode_index", 2)
        val mode = when (savedThemeIndex) {
            0 -> AppCompatDelegate.MODE_NIGHT_NO
            1 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        // --- 2. FONT SIZE LOGIC ---
        val fontSize = sharedPref.getString("font_size_key", "Medium")
        when (fontSize) {
            "Small" -> setTheme(R.style.Theme_Urbanfix_Small)
            "Large" -> setTheme(R.style.Theme_Urbanfix_Large)
            else -> setTheme(R.style.Theme_Urbanfix_Medium)
        }

        // --- 3. FONT STYLE LOGIC (Corrected) ---
        val fontStyleIndex = sharedPref.getInt("font_style_index", 0)
        when (fontStyleIndex) {
            1 -> setTheme(R.style.Theme_Urbanfix_Serif)
            2 -> setTheme(R.style.Theme_Urbanfix_Mono)
            else -> setTheme(R.style.Theme_Urbanfix_Sans)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bottom_layout)

        // --- Rest of your UI Setup (Untouched) ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_profile, R.id.navigation_report, R.id.navigation_complaints)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // Hides Bottom Navigation when in Settings
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_settings) {
                bottomNavigationView.visibility = View.GONE
            } else {
                bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.navigation_settings)
                true
            }
            R.id.action_help -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("UrbanFix Help")
                    .setIcon(android.R.drawable.ic_menu_help)
                    .setMessage("How can we help you today?\n\n• For technical issues: support@urbanfix.com\n• For complaints: Use the Complaints tab.\n• Version: 1.0.0")
                    .setPositiveButton("Dismiss") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}