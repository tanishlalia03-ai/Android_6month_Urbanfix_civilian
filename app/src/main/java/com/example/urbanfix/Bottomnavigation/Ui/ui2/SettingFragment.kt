package com.example.urbanfix.Bottomnavigation.Ui.ui2

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// Passing the layout to the constructor is the easiest way to inflate the UI
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

        // 3. Set Up Click Listeners
        themeLayout?.setOnClickListener {
            val options = arrayOf("Light", "Dark", "System Default")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("App Theme")
                .setItems(options) { _, which ->
                    tvTheme?.text = options[which]
                }.show()
        }

        fontLayout?.setOnClickListener {
            val options = arrayOf("Small", "Medium", "Large")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Font Size")
                .setItems(options) { _, which ->
                    tvFontSize?.text = options[which]
                }.show()
        }

        styleLayout?.setOnClickListener {
            val options = arrayOf("Sans Serif", "Serif", "Monospace")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Font Style")
                .setItems(options) { _, which ->
                    tvFontStyle?.text = options[which]
                }.show()
        }
    }
}