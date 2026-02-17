package com.example.urbanfix.Bottomnavigation.Ui.ui2

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.urbanfix.databinding.FragmentEditprofileBinding

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditprofileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditprofileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etEditName.text.toString().trim()
            val email = binding.etEditEmail.text.toString().trim()
            val mobile = binding.etEditMobile.text.toString().trim()

            when {
                name.isEmpty() -> {
                    binding.etEditName.error = "Name cannot be empty"
                    binding.etEditName.requestFocus()
                }
                email.isEmpty() -> {
                    binding.etEditEmail.error = "Email cannot be empty"
                    binding.etEditEmail.requestFocus()
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.etEditEmail.error = "Invalid email format"
                    binding.etEditEmail.requestFocus()
                }
                mobile.isEmpty() -> {
                    binding.etEditMobile.error = "Mobile number required"
                    binding.etEditMobile.requestFocus()
                }
                mobile.length != 10 -> {
                    binding.etEditMobile.error = "Mobile must be 10 digits"
                    binding.etEditMobile.requestFocus()
                }
                else -> {
                    Toast.makeText(requireContext(), "Changes Saved!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
