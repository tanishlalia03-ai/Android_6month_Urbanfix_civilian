package com.example.urbanfix.bottomnavigation.ui.ui2

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.example.urbanfix.mode.UrbanFixApp // Import your combined App class
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingFragment : Fragment(R.layout.fragment_setting) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeLayout = view.findViewById<LinearLayout>(R.id.layout_theme)
        val fontLayout = view.findViewById<LinearLayout>(R.id.layout_font_size)
        val styleLayout = view.findViewById<LinearLayout>(R.id.layout_font_style)

        val tvTheme = view.findViewById<TextView>(R.id.tv_current_theme)
        val tvFontSize = view.findViewById<TextView>(R.id.tv_current_font_size)
        val tvFontStyle = view.findViewById<TextView>(R.id.tv_current_font_style)

        val sharedPref = requireActivity().getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // --- Initial UI Setup ---
        val savedFontSize = sharedPref.getString("font_size_key", "Medium") ?: "Medium"
        tvFontSize?.text = savedFontSize
        applySizeToView(view as ViewGroup, savedFontSize)

        val themeOptions = arrayOf("Light", "Dark", "System Default")
        val savedThemeIndex = sharedPref.getInt("theme_mode_index", 2)
        tvTheme?.text = themeOptions[savedThemeIndex]

        val styleOptions = arrayOf("Sans Serif", "Serif", "Monospace")
        val savedStyleIndex = sharedPref.getInt("font_style_index", 0)
        tvFontStyle?.text = styleOptions[savedStyleIndex]

        // --- Theme Selection ---
        themeLayout?.setOnClickListener {
            val currentTheme = sharedPref.getInt("theme_mode_index", 2)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("App Theme")
                .setSingleChoiceItems(themeOptions, currentTheme) { dialog, which ->
                    // 1. Save the choice
                    sharedPref.edit().putInt("theme_mode_index", which).apply()

                    // 2. Update UI Text
                    tvTheme?.text = themeOptions[which]

                    // 3. Apply immediately using the static helper in UrbanFixApp
                    UrbanFixApp.applyTheme(requireContext())

                    dialog.dismiss()
                }
                .show()
        }

        // --- Font Size Selection ---
        fontLayout?.setOnClickListener {
            val options = arrayOf("Small", "Medium", "Large")
            val currentSize = sharedPref.getString("font_size_key", "Medium")
            val checkedItem = options.indexOf(currentSize)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Font Size")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    sharedPref.edit().putString("font_size_key", options[which]).apply()
                    dialog.dismiss()
                    // Recreate activity to apply the new XML theme style
                    requireActivity().recreate()
                }
                .show()
        }

        // --- Font Style Selection ---
        styleLayout?.setOnClickListener {
            val options = arrayOf("Sans Serif", "Serif", "Monospace")
            val checkedItem = sharedPref.getInt("font_style_index", 0)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Font Style")
                .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                    sharedPref.edit().putInt("font_style_index", which).apply()
                    dialog.dismiss()
                    requireActivity().recreate()
                }.show()
        }
    }

    private fun applySizeToView(viewGroup: ViewGroup, sizeLabel: String) {
        val size = when (sizeLabel) {
            "Small" -> 14f
            "Large" -> 22f
            else -> 18f
        }
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView) {
                child.textSize = size
            } else if (child is ViewGroup) {
                applySizeToView(child, sizeLabel)
            }
        }
    }
}