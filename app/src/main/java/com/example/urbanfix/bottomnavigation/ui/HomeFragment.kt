package com.example.urbanfix.bottomnavigation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.example.urbanfix.R
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment(R.layout.fragment_home), OnMapReadyCallback {

    private var tvUserName: TextView? = null
    private var tvPending: TextView? = null
    private var tvProgress: TextView? = null
    private var tvCompleted: TextView? = null
    private var pieChart: PieChart? = null
    private var mGoogleMap: GoogleMap? = null
    private var homeScrollView: NestedScrollView? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private var complaintsListener: ValueEventListener? = null
    private var complaintsRef: DatabaseReference? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) getCurrentLocationAndMoveCamera()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvUserName = view.findViewById(R.id.tv_user_name)
        tvPending = view.findViewById(R.id.tv_count_pending)
        tvProgress = view.findViewById(R.id.tv_count_progress)
        tvCompleted = view.findViewById(R.id.tv_count_completed)
        pieChart = view.findViewById(R.id.complaintsPieChart)

        // Find the ScrollView and Map Container for touch logic
        homeScrollView = view.findViewById(R.id.home_scroll_view)
        val mapContainer = view.findViewById<View>(R.id.map_container)

        // --- TOUCH INTERCEPT LOGIC ---
        // Stops the page from scrolling when you move the map
        mapContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    homeScrollView?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    homeScrollView?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> false
            }
        }

        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        val uid = auth.currentUser?.uid ?: return
        fetchUserName(uid)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isTiltGesturesEnabled = true
            isRotateGesturesEnabled = true
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
        }

        checkPermissionAndLoadMap()

        val uid = auth.currentUser?.uid ?: return
        listenToComplaints(uid)
    }

    private fun checkPermissionAndLoadMap() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndMoveCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndMoveCamera() {
        try {
            mGoogleMap?.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                } else {
                    mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(31.3260, 75.5762), 11f))
                }
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun listenToComplaints(uid: String) {
        complaintsRef = db.getReference("Complaints")
        complaintsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || mGoogleMap == null) return

                var p = 0; var pr = 0; var c = 0
                mGoogleMap?.clear()

                for (ds in snapshot.children) {
                    val status = ds.child("status").getValue(Int::class.java) ?: 0
                    val cId = ds.child("civilianId").getValue(String::class.java)
                    val lat = ds.child("latitude").getValue(Double::class.java)
                    val lng = ds.child("longitude").getValue(Double::class.java)

                    if (cId == uid) {
                        when (status) {
                            0 -> p++; 1 -> pr++; 2 -> c++
                        }
                    }

                    if (lat != null && lng != null) {
                        val hue = when(status) {
                            0 -> BitmapDescriptorFactory.HUE_AZURE   // Pending
                            1 -> BitmapDescriptorFactory.HUE_ORANGE  // Progress
                            else -> BitmapDescriptorFactory.HUE_CYAN // Solved
                        }

                        mGoogleMap?.addMarker(MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title("Status: ${if(status==0) "Pending" else if(status==1) "Active" else "Solved"}")
                            .icon(BitmapDescriptorFactory.defaultMarker(hue)))
                    }
                }

                tvPending?.text = String.format("%02d", p)
                tvProgress?.text = String.format("%02d", pr)
                tvCompleted?.text = String.format("%02d", c)

                updatePieChart(p, pr, c)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        complaintsRef?.addValueEventListener(complaintsListener!!)
    }

    private fun fetchUserName(uid: String) {
        db.getReference("Users").child(uid).child("name").get().addOnSuccessListener {
            if (isAdded) tvUserName?.text = it.getValue(String::class.java) ?: "User"
        }
    }
    private fun updatePieChart(p: Int, pr: Int, c: Int) {
        val entries = ArrayList<PieEntry>()
        if (p > 0) entries.add(PieEntry(p.toFloat(), "Pending"))
        if (pr > 0) entries.add(PieEntry(pr.toFloat(), "Active"))
        if (c > 0) entries.add(PieEntry(c.toFloat(), "Solved"))

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#42A5F5"), // Azure
                Color.parseColor("#FFA726"), // Orange
                Color.parseColor("#26C6DA")  // Cyan
            )
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }

        pieChart?.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        complaintsListener?.let { complaintsRef?.removeEventListener(it) }
        tvUserName = null; tvPending = null; tvProgress = null; tvCompleted = null
        pieChart = null; mGoogleMap = null; homeScrollView = null
        super.onDestroyView()
    }
}