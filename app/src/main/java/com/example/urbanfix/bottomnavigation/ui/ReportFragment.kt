package com.example.urbanfix.bottomnavigation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.example.urbanfix.fcm.NotificationSender
import com.example.urbanfix.firebase.ComplaintModel
import com.example.urbanfix.recyclerviewImage.ImagePreviewAdapter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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

    private val TAG = "URBANFIX_LOG"

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

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                textClassifier = NLClassifier.createFromFile(requireContext(), "text_classification.tflite")
            } catch (e: Exception) { Log.e(TAG, "Model Load Fail: ${e.message}") }
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

    private fun handleSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(context, "Please fill in all details", Toast.LENGTH_SHORT).show()
            return
        }

        val blacklist = arrayOf(
            "stupid", "idiot", "dumb", "pathetic", "useless", "nonsense", "crazy",
            "fool", "moron", "retard", "shut up", "get lost",
            "incompetent", "lazy", "hell", "pissed", "annoying",
            "fuck", "fucking", "bitch", "bastard", "asshole", "dick",
            "piss", "pissed", "slut", "whore",
        )
        val isManualUnprofessional = blacklist.any { desc.contains(it, ignoreCase = true) }

        if (isManualUnprofessional) {
            binding.etDescription.error = "Please avoid using rude or unprofessional language."
            Toast.makeText(context, "Unprofessional language detected", Toast.LENGTH_SHORT).show()
            return
        }

        textClassifier?.let { classifier ->
            val results = classifier.classify(desc)
            val topResult = results.maxByOrNull { it.score }
            if (topResult?.label == "Negative" && topResult.score > 0.4) {
                binding.etDescription.error = "Our AI detected a rude tone. Please be more professional."
                Toast.makeText(context, "AI Filter: Negative tone detected", Toast.LENGTH_SHORT).show()
                return
            }
        }

        startSubmissionProcess()
    }

    private fun startSubmissionProcess() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val civilianId = user.uid

        if (selectedImagesList.isEmpty()) {
            Toast.makeText(context, "Please add at least one image", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "Submitting..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urls = selectedImagesList.mapNotNull { appwriteManager.uploadAndGetUrl(requireContext(), bucketID, it) }
                val dbRef = FirebaseDatabase.getInstance().getReference("Complaints")

                // --- GENERATE SYSTEMATIC COMPLAINT ID ---
                val datePart = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
                val randomPart = UUID.randomUUID().toString().substring(0, 4).uppercase()
                val systematicKey = "UF-$datePart-$randomPart"
                // ----------------------------------------

                val priorityValue = when(binding.priorityToggle.checkedButtonId) {
                    R.id.btnHigh -> 2
                    R.id.btnMedium -> 1
                    else -> 0
                }

                val reportTitle = binding.etTitle.text.toString().trim()
                val reportCategory = binding.categorySpinner.text.toString()

                val complaint = ComplaintModel(
                    complaintId = systematicKey, // Using the new key
                    title = reportTitle,
                    description = binding.etDescription.text.toString(),
                    issueType = reportCategory,
                    images = ArrayList(urls),
                    civilianId = civilianId,
                    latitude = latitude,
                    longitude = longitude,
                    timeStamp = System.currentTimeMillis(),
                    status = 0,
                    priority = priorityValue,
                    location = binding.tvSelectedLocation.text.toString()
                )

                dbRef.child(systematicKey).setValue(complaint).addOnSuccessListener {

                    val adminMsgRef = FirebaseDatabase.getInstance().getReference("AdminMessages").push()
                    adminMsgRef.setValue(mapOf(
                        "title" to "New Report: $reportTitle",
                        "body" to "ID: $systematicKey | Category: $reportCategory",
                        "complaintId" to systematicKey,
                        "civilianId" to civilianId
                    ))

                    NotificationSender.sendNotificationToUser(
                        fcmToken = "/topics/admin_notifications",
                        title = "UrbanFix: $reportTitle",
                        body = "New $reportCategory reported ($systematicKey)",
                        key = systematicKey,
                        context = requireContext(),
                        type = "complaint_report",
                        name = user.displayName ?: "Citizen",
                        civilianId = civilianId
                    )

                    lifecycleScope.launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Report Submitted Successfully!", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.text = "Submit"
                    Log.e(TAG, "Submission Error: ${e.message}")
                }
            }
        }
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
            Toast.makeText(context, "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
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
        input.hint = "e.g. Model Town, Jalandhar"
        AlertDialog.Builder(requireContext())
            .setTitle("Manual Location")
            .setView(input)
            .setPositiveButton("Set") { _, _ ->
                val addressStr = input.text.toString().trim()
                if (addressStr.isNotEmpty()) {
                    binding.tvSelectedLocation.text = "Searching..."
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(requireContext(), Locale.getDefault())
                            val addresses = geocoder.getFromLocationName(addressStr, 1)
                            withContext(Dispatchers.Main) {
                                if (!addresses.isNullOrEmpty()) {
                                    latitude = addresses[0].latitude
                                    longitude = addresses[0].longitude
                                    binding.tvSelectedLocation.text = addressStr
                                }
                            }
                        } catch (e: Exception) { }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchGPSLocation()
        else requestLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @SuppressLint("MissingPermission")
    private fun fetchGPSLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.tvSelectedLocation.text = "Detecting Location..."
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
            loc?.let {
                this.latitude = it.latitude; this.longitude = it.longitude
                lifecycleScope.launch { binding.tvSelectedLocation.text = getAddressFromCoords(it.latitude, it.longitude) }
            }
        }
    }

    private suspend fun getAddressFromCoords(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown Address"
        } catch (e: Exception) { "Lat: $lat, Lng: $lng" }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Pothole & Road Damage", "Garbage & Waste", "Street Light Outage", "Water Leak & Pipe Burst", "Sewage & Clogged Drains", "Illegal Encroachment", "Park & Playground Maintenance", "Stray Animal Menace", "Electricity Fault", "Illegal Construction", "Public Toilet Hygiene", "Dangling Wires", "Noise Pollution", "Dead Animal Removal","Traffic Signal Failure", "Other")
        binding.categorySpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}