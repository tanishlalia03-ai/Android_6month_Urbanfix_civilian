package com.example.urbanfix.Bottomnavigation.Ui.ui2

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingFragment : Fragment(R.layout.fragment_setting) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Link the Layouts (Rows)
        val themeLayout = view.findViewById<LinearLayout>(R.id.layout_theme)
        val fontLayout = view.findViewById<LinearLayout>(R.id.layout_font_size)
        val styleLayout = view.findViewById<LinearLayout>(R.id.layout_font_style)

        // 2. Link the TextViews (Values)
        val tvTheme = view.findViewById<TextView>(R.id.tv_current_theme)
        val tvFontSize = view.findViewById<TextView>(R.id.tv_current_font_size)
        val tvFontStyle = view.findViewById<TextView>(R.id.tv_current_font_style)

        val sharedPref = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // 3. Initial UI Setup (Loading all saved values)
        // Font Size
        val savedFontSize = sharedPref.getString("font_size_key", "Medium")
        tvFontSize?.text = savedFontSize

        // Theme
        val themeOptions = arrayOf("Light", "Dark", "System Default")
        val savedThemeIndex = sharedPref.getInt("theme_mode_index", 2)
        tvTheme?.text = themeOptions[savedThemeIndex]

        // Font Style
        val styleOptions = arrayOf("Sans Serif", "Serif", "Monospace")
        val savedStyleIndex = sharedPref.getInt("font_style_index", 0)
        tvFontStyle?.text = styleOptions[savedStyleIndex]

        // 4. Set Up Alert Dialog for Font Size (Your original logic)
        fontLayout?.setOnClickListener {
            val options = arrayOf("Small", "Medium", "Large")
            val checkedItem = options.indexOf(tvFontSize?.text.toString())

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Font Size")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    val selectedSize = options[which]
                    sharedPref.edit().putString("font_size_key", selectedSize).apply()
                    tvFontSize?.text = selectedSize
                    dialog.dismiss()
                    requireActivity().recreate()
                }
                .show()
        }

        // --- Dialog for App Theme ---
        themeLayout?.setOnClickListener {
            val options = arrayOf("Light", "Dark", "System Default")
            val checkedItem = sharedPref.getInt("theme_mode_index", 2)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("App Theme")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    tvTheme?.text = options[which]
                    sharedPref.edit().putInt("theme_mode_index", which).apply()

                    val mode = when (which) {
                        0 -> AppCompatDelegate.MODE_NIGHT_NO
                        1 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    AppCompatDelegate.setDefaultNightMode(mode)
                    dialog.dismiss()
                }
                .show()
        }

        // --- Dialog for Font Style ---
        styleLayout?.setOnClickListener {
            val options = arrayOf("Sans Serif", "Serif", "Monospace")
            val checkedItem = sharedPref.getInt("font_style_index", 0)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Font Style")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    tvFontStyle?.text = options[which]
                    sharedPref.edit().putInt("font_style_index", which).apply()
                    dialog.dismiss()
                    // Recreates activity to apply the Font Style theme
                    requireActivity().recreate()
                }.show()
        }
    }
}