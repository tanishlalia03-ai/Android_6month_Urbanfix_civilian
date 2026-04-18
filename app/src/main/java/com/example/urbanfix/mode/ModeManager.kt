package com.example.urbanfix.mode

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

// This class acts as BOTH the Application and the Logic Manager
class UrbanFixApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // This forces the theme the second the app process starts
        applyTheme(this)
    }

    companion object {
        fun applyTheme(context: Context) {
            val sharedPref = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val themeIndex = sharedPref.getInt("theme_mode_index", 2)

            val mode = when (themeIndex) {
                0 -> AppCompatDelegate.MODE_NIGHT_NO
                1 -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }
}