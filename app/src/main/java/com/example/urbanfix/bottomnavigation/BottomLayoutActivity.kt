package com.example.urbanfix.bottomnavigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.urbanfix.R
import com.example.urbanfix.firebase.UrbanFixNotificationManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BottomLayoutActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val TAG = "UrbanFix_Notif"

    // Notification Variables
    private var broadcastRef: DatabaseReference? = null
    private var broadcastListener: ChildEventListener? = null
    private val knownBroadcastKeys = mutableSetOf<String>()

    private var civilianMessageRef: DatabaseReference? = null
    private var civilianMessageListener: ChildEventListener? = null
    private val knownCivilianMessageKeys = mutableSetOf<String>()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Notifications are disabled. You won't get alerts.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE)

        // Theme Logic
        val fontSize = sharedPref.getString("font_size_key", "Medium")
        val fontStyleIndex = sharedPref.getInt("font_style_index", 0)

        when {
            fontSize == "Small" -> setTheme(R.style.Theme_Urbanfix_Small)
            fontSize == "Large" -> setTheme(R.style.Theme_Urbanfix_Large)
            fontStyleIndex == 1 -> setTheme(R.style.Theme_Urbanfix_Serif)
            fontStyleIndex == 2 -> setTheme(R.style.Theme_Urbanfix_Mono)
            else -> setTheme(R.style.Theme_Urbanfix_Medium)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bottom_layout)

        // UI Setup
        window.statusBarColor = getColor(R.color.blue_main)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Navigation Setup
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_report, R.id.navigation_complaints, R.id.navigation_notifications, R.id.navigation_profile)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_settings, R.id.navigation_edit_profile, R.id.navigation_view_detail -> {
                    bottomNavigationView.visibility = View.GONE
                }
                else -> bottomNavigationView.visibility = View.VISIBLE
            }
        }

        // --- NOTIFICATION INITIALIZATION ---
        UrbanFixNotificationManager.ensureChannel(this)
        requestNotificationPermissionIfNeeded()
        startBroadcastListener()
        startCivilianMessageListener()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- BROADCAST LOGIC (Everyone) ---
    private fun startBroadcastListener() {
        val ref = FirebaseDatabase.getInstance().getReference("BroadcastMessages")
        broadcastRef = ref
        ref.get().addOnSuccessListener { snapshot ->
            knownBroadcastKeys.clear()
            snapshot.children.mapNotNullTo(knownBroadcastKeys) { it.key }
            attachBroadcastListener(ref)
        }.addOnFailureListener { attachBroadcastListener(ref) }
    }

    private fun attachBroadcastListener(ref: DatabaseReference) {
        if (broadcastListener != null) return
        broadcastListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key.orEmpty()
                // Prevent duplicate pop-ups for old messages
                if (key.isNotBlank() && !knownBroadcastKeys.add(key)) return

                val title = snapshot.child("title").value?.toString().orEmpty()
                val body = snapshot.child("body").value?.toString().orEmpty()

                if (title.isNotEmpty()) {
                    UrbanFixNotificationManager.show(this@BottomLayoutActivity, title, body)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, p1: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Broadcast Listener Cancelled: ${error.message}")
            }
        }
        ref.addChildEventListener(broadcastListener!!)
    }

    // --- CIVILIAN LOGIC (Personal) ---
    private fun startCivilianMessageListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("CivilianMessages")
        civilianMessageRef = ref
        ref.get().addOnSuccessListener { snapshot ->
            knownCivilianMessageKeys.clear()
            snapshot.children.mapNotNullTo(knownCivilianMessageKeys) { it.key }
            attachCivilianMessageListener(ref, uid)
        }.addOnFailureListener { attachCivilianMessageListener(ref, uid) }
    }

    private fun attachCivilianMessageListener(ref: DatabaseReference, currentUid: String) {
        if (civilianMessageListener != null) return
        civilianMessageListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key.orEmpty()
                if (key.isNotBlank() && !knownCivilianMessageKeys.add(key)) return

                val civilianIdFromDB = snapshot.child("civilianId").value?.toString().orEmpty()

                // Debug log to compare IDs in Logcat
                Log.d(TAG, "Comparing DB UID: $civilianIdFromDB with Current UID: $currentUid")

                // Only show if the ID matches exactly (ignoring case for safety)
                if (civilianIdFromDB.equals(currentUid, ignoreCase = true)) {
                    val title = snapshot.child("title").value?.toString() ?: "Status Update"
                    val body = snapshot.child("body").value?.toString().orEmpty()

                    UrbanFixNotificationManager.show(this@BottomLayoutActivity, title, body)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, p1: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, p1: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Civilian Listener Cancelled: ${error.message}")
            }
        }
        ref.addChildEventListener(civilianMessageListener!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Null-safe cleanup of listeners
        broadcastListener?.let { broadcastRef?.removeEventListener(it) }
        civilianMessageListener?.let { civilianMessageRef?.removeEventListener(it) }
    }

    // --- MENU CODE ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.navigation_settings)
                true
            }
            R.id.action_help -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle("UrbanFix Help")
                    .setMessage("Contact support@urbanfix.com")
                    .setPositiveButton("Dismiss") { d, _ -> d.dismiss() }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()
}