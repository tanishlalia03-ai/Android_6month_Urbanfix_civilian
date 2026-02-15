package com.example.urbanfix.Bottomnavigation.Ui.ui2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // IMPORTANT: Use this import
import com.example.urbanfix.R
import com.google.android.material.button.MaterialButton

class EditProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editprofile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure this ID matches your fragment_editprofile.xml
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveProfile)

        btnSave.setOnClickListener {
            Toast.makeText(requireContext(), "Changes Saved!", Toast.LENGTH_SHORT).show()

            // PROPER WAY TO GO BACK:
            // This pops the Edit screen and shows the Profile screen again
            findNavController().popBackStack()
        }
    }
}