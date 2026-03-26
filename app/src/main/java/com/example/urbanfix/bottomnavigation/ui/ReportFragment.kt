package com.example.urbanfix.bottomnavigation.ui

import android.Manifest
import android.annotation.SuppressLint
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
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchGPSLocation()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryDropdown()

        // Background AI Init
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                textClassifier = NLClassifier.createFromFile(requireContext(), "text_classification.tflite")
            } catch (e: Exception) { e.printStackTrace() }
        }

        binding.btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
            else requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        binding.btnCurrentLoc.setOnClickListener { checkLocationPermissions() }
        binding.btnManualLoc.setOnClickListener { showManualLocationDialog() }

        setupDateTimePickers()
        binding.btnSubmit.setOnClickListener { handleSubmit() }
    }

    private fun openCamera() {
        val photoFile = File(requireContext().cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        val authority = "${requireContext().applicationContext.packageName}.fileprovider"
        tempCameraUri = FileProvider.getUriForFile(requireContext(), authority, photoFile)
        takePhotoLauncher.launch(tempCameraUri)
    }

    private fun setupRecyclerView() {
        imageAdapter = ImagePreviewAdapter(selectedImagesList)
        binding.rvImagePreview.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
    }

    private fun addImageToList(uri: Uri) {
        if (selectedImagesList.size < 3) {
            selectedImagesList.add(uri)
            imageAdapter.notifyItemInserted(selectedImagesList.size - 1)
        } else {
            Toast.makeText(context, "Max 3 images", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDateTimePickers() {
        binding.btnPickDate.setOnClickListener {
            val dp = MaterialDatePicker.Builder.datePicker().build()
            dp.show(parentFragmentManager, "DATE")
            dp.addOnPositiveButtonClickListener {
                selectedDate = SimpleDateFormat("dd/MMM/yyyy", Locale.getDefault()).format(Date(it))
                updateDateTimeUI()
            }
        }
        binding.btnPickTime.setOnClickListener {
            val tp = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H).build()
            tp.show(parentFragmentManager, "TIME")
            tp.addOnPositiveButtonClickListener {
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", tp.hour, tp.minute)
                updateDateTimeUI()
            }
        }
    }

    private fun updateDateTimeUI() {
        binding.tvSelectedDateTime.text = "$selectedDate $selectedTime".trim()
    }

    private fun showManualLocationDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Manual Location")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                binding.tvSelectedLocation.text = input.text.toString()
            }
            .show()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchGPSLocation()
        } else {
            requestLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchGPSLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.tvSelectedLocation.text = "Locating..."

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                loc?.let {
                    this.latitude = it.latitude
                    this.longitude = it.longitude

                    lifecycleScope.launch {
                        val address = getAddressFromCoords(it.latitude, it.longitude)
                        binding.tvSelectedLocation.text = address
                    }
                }
            }
    }

    private suspend fun getAddressFromCoords(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.getAddressLine(0) ?: "Address not found"
            } catch (e: Exception) {
                "Lat: $lat, Lng: $lng"
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Pothole & Road Damage", "Garbage & Waste", "Street Light Outage", "Water Leak & Pipe Burst", "Sewage & Clogged Drains", "Other")
        binding.categorySpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
    }

    private fun handleSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Title required"
            return
        }
        if (desc.isEmpty()) {
            binding.etDescription.error = "Description required"
            return
        }

        // --- MANUAL WRONG WORD CHECK ---
        val wrongWords = arrayOf("stupid", "idiot", "dumb", "fool", "nonsense", "rubbish", "crap",
            "hell", "damn", "suck", "trash","worthless")
        for (word in wrongWords) {
            if (desc.lowercase().contains(word)) {
                binding.etDescription.error = "Inappropriate wording: '$word' detected"
                binding.etDescription.requestFocus()
                return
            }
        }

        // --- AI WORDING CHECK ---
        textClassifier?.let { classifier ->
            val results = classifier.classify(desc)
            val topResult = results.maxByOrNull { it.score }

            if (topResult?.label == "Negative" && topResult.score > 0.45) {
                binding.etDescription.error = "Please use professional wording (AI review active)"
                binding.etDescription.requestFocus()
                return
            }
        }

        startSubmissionProcess()
    }

    private fun startSubmissionProcess() {
        val civilianId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (selectedImagesList.isEmpty()) {
            Toast.makeText(context, "Add images", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urls = selectedImagesList.mapNotNull { appwriteManager.uploadAndGetUrl(requireContext(), bucketID, it) }

                val complaintRef = FirebaseDatabase.getInstance().getReference("Complaints")
                val adminMsgRef = FirebaseDatabase.getInstance().getReference("AdminMessages")
                val key = complaintRef.push().key ?: ""

                val priority = when(binding.priorityToggle.checkedButtonId) {
                    R.id.btnHigh -> 2
                    R.id.btnMedium -> 1
                    else -> 0
                }

                val complaint = ComplaintModel(
                    complaintId = key,
                    title = binding.etTitle.text.toString(),
                    description = binding.etDescription.text.toString(),
                    issueType = binding.categorySpinner.text.toString(),
                    images = ArrayList(urls),
                    civilianId = civilianId,
                    latitude = latitude,
                    longitude = longitude,
                    timeStamp = System.currentTimeMillis(),
                    status = 0,
                    priority = priority,
                    location = binding.tvSelectedLocation.text.toString()
                )

                withContext(Dispatchers.Main) {
                    complaintRef.child(key).setValue(complaint).addOnSuccessListener {

                        // --- SEND DATA TO ADMIN NODE FOR FCM ---
                        val adminNotification = mapOf(
                            "title" to "New Report: ${complaint.title}",
                            "body" to "Category: ${complaint.issueType}",
                            "complaintId" to key
                        )
                        adminMsgRef.push().setValue(adminNotification)
                        // ----------------------------------------

                        Toast.makeText(requireContext(), "Submitted!", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
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