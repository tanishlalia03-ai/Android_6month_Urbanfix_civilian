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
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
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

    // Advanced AI Variable
    private var textClassifier: NLClassifier? = null

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

        // Initialize Advanced AI in Background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                textClassifier = NLClassifier.createFromFile(requireContext(), "text_classification.tflite")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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

    // --- HYBRID AI + MANUAL SUBMIT ---
    private fun handleSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) { binding.etTitle.error = "Required"; return }
        if (description.isEmpty()) { binding.etDescription.error = "Required"; return }

        var isAbusive = false

        // 1. Layer 1: AI Sentiment Moderation (Strict 0.4 Threshold)
        textClassifier?.let { ai ->
            val results = ai.classify(description)
            val category = results.find {
                it.label.contains("Negative", ignoreCase = true) ||
                        it.label.contains("Toxic", ignoreCase = true)
            }
            if ((category?.score ?: 0f) > 0.4f) {
                isAbusive = true
            }
        }

        // 2. Layer 2: Manual Pattern Matching
        if (!isAbusive && containsProhibitedWords(description)) {
            isAbusive = true
        }

        if (isAbusive) {
            Toast.makeText(requireContext(),
                "To keep our community helpful, please use professional language and then again submit.",
                Toast.LENGTH_LONG).show()

            binding.etDescription.error = "Please review your wording"
            binding.etDescription.requestFocus()
            return
        }

        startSubmissionProcess()
    }

    private fun containsProhibitedWords(text: String): Boolean {
        val badWords = listOf(
            "abuse", "idiot", "stupid", "fraud", "scam", "fucking", "bastard",
            "shit", "bitch", "asshole", "piss", "dick", "pussy", "fake", "nonsense","useless", "dumb", "moron", "loser","liar", "corrupt", "bribe", "ghoos", "spam", "advertisement",
            "f*ck", "sh*t", "a$$", "b*tch", "sc@m", "fr@ud"
        )

        val cleanInput = text.lowercase(Locale.getDefault())
            .replace(" ", "")
            .replace(".", "")
            .replace("*", "")
            .replace("-", "")
            .replace("_", "")
            .replace("@", "a")
            .replace("0", "o")
            .replace("1", "i")

        return badWords.any { cleanInput.contains(it) }
    }

    private fun startSubmissionProcess() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val issueType = binding.categorySpinner.text.toString()
        val civilianId = FirebaseAuth.getInstance().currentUser?.uid
        val priority = if (binding.urgencyRadioGroup.checkedRadioButtonId == R.id.radioHigh) 2 else 1

        if (selectedImagesList.isEmpty() || civilianId == null) {
            Toast.makeText(requireContext(), "Check images or login status", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        Toast.makeText(requireContext(), "Verification Successful. Uploading...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uploadedUrls = ArrayList<String>()
                selectedImagesList.forEach { uri ->
                    val url = appwriteManager.uploadAndGetUrl(requireContext(), bucketID, uri)
                    url?.let { uploadedUrls.add(it) }
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
                val adminMsgRef = FirebaseDatabase.getInstance().getReference("AdminMessages")
                val complaintKey = dbRef.push().key ?: UUID.randomUUID().toString()

                withContext(Dispatchers.Main) {
                    dbRef.child(complaintKey).setValue(complaint.copy(complaintId = complaintKey))
                        .addOnSuccessListener {

                            // --- ADMIN NOTIFICATION TRIGGER WITH LINKED ID ---
                            val notificationData = mapOf(
                                "title" to "New Report: $title",
                                "body" to "A new $issueType report has been filed.",
                                "complaintId" to complaintKey, // Links both root nodes
                                "timestamp" to ServerValue.TIMESTAMP,
                                "status" to "unread"
                            )
                            adminMsgRef.push().setValue(notificationData)

                            Toast.makeText(requireContext(), "Report Submitted! Admin Notified.", Toast.LENGTH_LONG).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        .addOnFailureListener {
                            binding.btnSubmit.isEnabled = true
                            Toast.makeText(requireContext(), "Database Error", Toast.LENGTH_SHORT).show()
                        }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSubmit.isEnabled = true
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}