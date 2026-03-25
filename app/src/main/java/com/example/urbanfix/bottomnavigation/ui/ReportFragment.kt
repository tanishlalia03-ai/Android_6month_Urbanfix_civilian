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

        // Background AI Init
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                textClassifier = NLClassifier.createFromFile(requireContext(), "text_classification.tflite")
            } catch (e: Exception) { e.printStackTrace() }
        }

        // --- BUTTON LISTENERS ---
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
            .setPositiveButton("Set") { _, _ -> binding.tvSelectedLocation.text = input.text.toString() }
            .show()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetchGPSLocation()
        else requestLocationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    @androidx.annotation.RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun fetchGPSLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.tvSelectedLocation.text = "Locating..."
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
            loc?.let {
                latitude = it.latitude
                longitude = it.longitude
                binding.tvSelectedLocation.text = getAddressFromCoords(it.latitude, it.longitude)
            }
        }
    }

    private fun getAddressFromCoords(lat: Double, lng: Double): String {
        return try {
            val addresses = Geocoder(requireContext(), Locale.getDefault()).getFromLocation(lat, lng, 1)
            addresses?.get(0)?.getAddressLine(0) ?: "$lat, $lng"
        } catch (e: Exception) { "$lat, $lng" }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Pothole & Road Damage", "Garbage & Waste", "Street Light Outage", "Water Leak & Pipe Burst", "Sewage & Clogged Drains", "Other")
        binding.categorySpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))
    }

    private fun handleSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()

        if (title.isEmpty() || desc.isEmpty()) { Toast.makeText(context, "Fill details", Toast.LENGTH_SHORT).show(); return }

        // AI + Manual Check
        var toxic = false
        textClassifier?.let { ai ->
            val results = ai.classify(desc)
            if ((results.find { it.label.contains("Negative") }?.score ?: 0f) > 0.4f) toxic = true
        }
        if (toxic || listOf("abuse", "bad").any { desc.contains(it) }) {
            Toast.makeText(context, "Please use clean language", Toast.LENGTH_SHORT).show()
            return
        }

        startSubmissionProcess()
    }

    private fun startSubmissionProcess() {
        val civilianId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (selectedImagesList.isEmpty()) { Toast.makeText(context, "Add images", Toast.LENGTH_SHORT).show(); return }

        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urls = selectedImagesList.mapNotNull { appwriteManager.uploadAndGetUrl(requireContext(), bucketID, it) }
                val key = FirebaseDatabase.getInstance().getReference("Complaints").push().key ?: ""

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
                    FirebaseDatabase.getInstance().getReference("Complaints").child(key).setValue(complaint).addOnSuccessListener {
                        Toast.makeText(requireContext(), "Submitted!", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { binding.btnSubmit.isEnabled = true }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}