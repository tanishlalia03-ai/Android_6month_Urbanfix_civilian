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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
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
    private var mAdView: AdView? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private var complaintsListener: ValueEventListener? = null
    private var userComplaintsRef: DatabaseReference? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) getCurrentLocationAndMoveCamera()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MobileAds.initialize(requireContext()) {}
        mAdView = view.findViewById(R.id.adView)
        mAdView?.loadAd(AdRequest.Builder().build())

        tvUserName = view.findViewById(R.id.tv_user_name)
        tvPending = view.findViewById(R.id.tv_count_pending)
        tvProgress = view.findViewById(R.id.tv_count_progress)
        tvCompleted = view.findViewById(R.id.tv_count_completed)
        pieChart = view.findViewById(R.id.complaintsPieChart)
        homeScrollView = view.findViewById(R.id.home_scroll_view)
        val mapContainer = view.findViewById<View>(R.id.map_container)

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

        auth.currentUser?.uid?.let { fetchUserName(it) }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isZoomControlsEnabled = true
        }
        checkPermissionAndLoadMap()
        auth.currentUser?.uid?.let { listenToUserComplaints(it) }
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
                val currentLatLng = if (location != null) LatLng(location.latitude, location.longitude)
                else LatLng(31.3260, 75.5762)
                mGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun listenToUserComplaints(uid: String) {
        // FIX: Point specifically to the logged-in user's folder
        userComplaintsRef = db.getReference("Complaints").child(uid)

        complaintsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || mGoogleMap == null) return

                var p = 0; var pr = 0; var c = 0
                mGoogleMap?.clear()

                for (ds in snapshot.children) {
                    val status = ds.child("status").getValue(Int::class.java) ?: 0
                    val lat = ds.child("latitude").getValue(Double::class.java)
                    val lng = ds.child("longitude").getValue(Double::class.java)

                    // Increment counters based on status
                    when (status) {
                        0 -> p++
                        1 -> pr++
                        2 -> c++
                    }

                    // Add Markers to Map
                    if (lat != null && lng != null) {
                        val hue = when(status) {
                            0 -> BitmapDescriptorFactory.HUE_RED      // Pending
                            1 -> BitmapDescriptorFactory.HUE_ORANGE   // Active
                            else -> BitmapDescriptorFactory.HUE_GREEN // Solved
                        }

                        val statusLabel = when(status) {
                            0 -> "Pending"
                            1 -> "Active"
                            else -> "Solved"
                        }

                        mGoogleMap?.addMarker(MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title("Status: $statusLabel")
                            .icon(BitmapDescriptorFactory.defaultMarker(hue)))
                    }
                }

                // Update UI Texts
                tvPending?.text = String.format("%02d", p)
                tvProgress?.text = String.format("%02d", pr)
                tvCompleted?.text = String.format("%02d", c)

                updatePieChart(p, pr, c)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userComplaintsRef?.addValueEventListener(complaintsListener!!)
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

        if (entries.isEmpty()) {
            pieChart?.clear()
            return
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#EF5350"), // Red for Pending
                Color.parseColor("#FFA726"), // Orange for Active
                Color.parseColor("#66BB6A")  // Green for Solved
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

    override fun onPause() {
        mAdView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mAdView?.resume()
    }

    override fun onDestroyView() {
        mAdView?.destroy()
        complaintsListener?.let { userComplaintsRef?.removeEventListener(it) }

        // Clean up references for memory
        tvUserName = null; tvPending = null; tvProgress = null; tvCompleted = null
        pieChart = null; mGoogleMap = null; homeScrollView = null; mAdView = null
        super.onDestroyView()
    }
}