package com.example.urbanfix.Bottomnavigation.Ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Import this!
import com.example.urbanfix.R
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflates your ScrollView layout with John Doe's info
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. MATCH IDs FROM YOUR XML
        // XML ID was: android:id="@+id/btn_edit_profile"
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btn_edit_profile)

        // XML ID was: android:id="@+id/btn_logout"
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)

        // 2. NAVIGATE USING NAVGRAPH ACTION
        btnEditProfile.setOnClickListener {
            try {
                // This ID must match the <action> ID in your nav_graph.xml
                findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
            } catch (e: Exception) {
                // This helps you find the error if the ID in nav_graph is typed wrong
                Toast.makeText(requireContext(), "Check action ID in nav_graph!", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show()
        }
    }
}