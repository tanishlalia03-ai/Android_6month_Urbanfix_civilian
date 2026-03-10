package com.example.urbanfix.bottomnavigation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.urbanfix.R
import com.example.urbanfix.appwrite.AppwriteManager
import com.example.urbanfix.databinding.FragmentReportBinding
import com.example.urbanfix.firebase.ComplaintModel
import com.example.urbanfix.recyclerviewImage.ImagePreviewAdapter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    private val selectedImagesList = ArrayList<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter
    private val appwriteManager by lazy { AppwriteManager.getInstance(requireContext()) }

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var tempCameraUri: Uri? = null

    private val bucketID = "6996dc680036b04ee5f0"

    // --- LAUNCHERS ---

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { addImageToList(it) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) tempCameraUri?.let { addImageToList(it) }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) fetchGPSLocation()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryDropdown()

        binding.btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnCurrentLoc.setOnClickListener { checkLocationPermissions() }
        binding.btnManualLoc.setOnClickListener { showManualLocationDialog() }
        setupDateTimePickers()
        binding.btnSubmit.setOnClickListener { handleSubmit() }
    }

    private fun openCamera() {
        try {
            val photoFile = File(requireContext().cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
            val authority = "${requireContext().applicationContext.packageName}.fileprovider"
            tempCameraUri = FileProvider.getUriForFile(requireContext(), authority, photoFile)
            takePhotoLauncher.launch(tempCameraUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        imageAdapter = ImagePreviewAdapter(selectedImagesList)
        binding.rvImagePreview.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
    }

    private fun addImageToList(uri: Uri) {
        selectedImagesList.add(uri)
        imageAdapter.notifyItemInserted(selectedImagesList.size - 1)
        binding.rvImagePreview.scrollToPosition(selectedImagesList.size - 1)
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchGPSLocation()
        } else {
            requestLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    @androidx.annotation.RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchGPSLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
            loc?.let {
                latitude = it.latitude
                longitude = it.longitude
                updateLocationUI("GPS Fix: ${getAddressFromCoords(it.latitude, it.longitude)}")
            } ?: Toast.makeText(context, "Turn on GPS and try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showManualLocationDialog() {
        val editText = EditText(requireContext()).apply { hint = "Enter address or landmark" }
        AlertDialog.Builder(requireContext())
            .setTitle("Manual Location")
            .setView(editText)
            .setPositiveButton("Set") { _, _ ->
                val manualAddress = editText.text.toString()
                if (manualAddress.isNotEmpty()) updateLocationUI(manualAddress)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getAddressFromCoords(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.get(0)?.getAddressLine(0) ?: "$lat, $lng"
        } catch (e: Exception) { "$lat, $lng" }
    }

    private fun updateLocationUI(text: String) {
        binding.tvSelectedLocation.text = text
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Pothole & Road Damage", "Garbage & Waste", "Street Light Outage", "Water Leak & Pipe Burst", "Sewage & Clogged Drains", "Other")
        binding.categorySpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
    }

    private fun setupDateTimePickers() {
        binding.btnPickDate.setOnClickListener {
            val dp = MaterialDatePicker.Builder.datePicker().build()
            dp.show(parentFragmentManager, "DP")
            dp.addOnPositiveButtonClickListener {
                selectedDate = SimpleDateFormat("dd/MMM/yyyy", Locale.getDefault()).format(Date(it))
                updateDateTimeDisplay()
            }
        }
        binding.btnPickTime.setOnClickListener {
            val tp = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).build()
            tp.show(parentFragmentManager, "TP")
            tp.addOnPositiveButtonClickListener {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", tp.hour, tp.minute)
                updateDateTimeDisplay()
            }
        }
    }

    private fun updateDateTimeDisplay() {
        binding.tvSelectedDateTime.text = if (selectedDate.isNotEmpty()) "$selectedDate | $selectedTime" else selectedTime
    }

    private fun handleSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val issueType = binding.categorySpinner.text.toString()
        val civilianId = FirebaseAuth.getInstance().currentUser?.uid

        val priority = when (binding.urgencyRadioGroup.checkedRadioButtonId) {
            R.id.radioHigh -> 2
            R.id.radioMedium -> 1
            else -> 0
        }

        // Validation
        if (title.isEmpty()) { binding.etTitle.error = "Required"; return }
        if (selectedImagesList.isEmpty()) {
            Toast.makeText(context, "Please add at least one image", Toast.LENGTH_SHORT).show()
            return
        }
        if (civilianId == null) {
            Toast.makeText(context, "Please sign in to report", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        Toast.makeText(context, "Uploading Report...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uploadedUrls = ArrayList<String>()

                // 1. Loop through and upload all images using the refined Manager
                selectedImagesList.forEach { uri ->
                    val url = appwriteManager.uploadAndGetUrl(requireContext(), bucketID, uri)
                    url?.let { uploadedUrls.add(it) }
                }

                // If some uploads failed, notify the user
                if (uploadedUrls.size != selectedImagesList.size) {
                    throw Exception("Failed to upload all images")
                }

                val complaint = ComplaintModel(
                    title = title,
                    description = description,
                    issueType = issueType,
                    images = uploadedUrls,
                    civilianId = civilianId,
                    latitude = latitude,
                    longitude = longitude,
                    timeStamp = System.currentTimeMillis(),
                    status = 0,
                    priority = priority,
                    location = binding.tvSelectedLocation.text.toString()
                )

                val dbRef = FirebaseDatabase.getInstance().getReference("Complaints")
                val complaintKey = dbRef.push().key ?: UUID.randomUUID().toString()

                // 2. Final Firebase Save on Main Thread
                withContext(Dispatchers.Main) {
                    dbRef.child(complaintKey).setValue(complaint.copy(complaintId = complaintKey))
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Report Submitted!", Toast.LENGTH_LONG).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        .addOnFailureListener {
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(requireContext(), "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(context, "Submission Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}