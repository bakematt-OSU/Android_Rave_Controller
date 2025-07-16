// src/main/java/com/example/android_rave_controller/MainActivity.kt
package com.example.android_rave_controller

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.android_rave_controller.databinding.ActivityMainBinding
import com.example.android_rave_controller.arduino_comm_ble.BluetoothDialogFragment
import com.example.android_rave_controller.arduino_comm_ble.ConnectionViewModel
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService // Corrected import
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val connectionViewModel: ConnectionViewModel by viewModels()

    // Declare properties to hold the views from the toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var textDeviceName: TextView
    private lateinit var imageConnectionStatus: ImageView
    private lateinit var statusLayout: LinearLayout

    // Add this declaration and initialization for appBarConfiguration
    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_segments
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize BluetoothService here before observing or using it
        BluetoothService.initialize(applicationContext)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Find the views inside the toolbar manually.
        // First, get a reference to the included layout.
        // The ID 'toolbar_content' is defined in activity_main.xml for the <include> tag.
        val toolbarContent = binding.toolbar.findViewById<View>(R.id.toolbar_content)

        // Now, find the actual TextViews and ImageView *within* that included layout
        toolbarTitle = toolbarContent.findViewById(R.id.toolbar_title)
        textDeviceName = toolbarContent.findViewById(R.id.text_device_name)
        imageConnectionStatus = toolbarContent.findViewById(R.id.image_connection_status)
        statusLayout = toolbarContent.findViewById(R.id.status_layout) // Find the layout

        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Listen for screen changes to update the title
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbarTitle.text = destination.label
        }

        // Observe the BluetoothService directly to update the ViewModel and the UI
        BluetoothService.connectionState.observe(this) { isConnected: Boolean ->
            val deviceName = if (isConnected) {
                BluetoothService.connectedDeviceName
            } else {
                null
            }
            connectionViewModel.updateConnection(isConnected, deviceName)
            updateConnectionStatus(isConnected, deviceName) // Directly update the UI here
        }

        // Add a click listener to the entire status layout
        statusLayout.setOnClickListener {
            BluetoothDialogFragment().show(supportFragmentManager, "BluetoothDialog")
        }

        // Set initial state
        updateConnectionStatus(false, null)
    }

    private fun updateConnectionStatus(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            // Connected State
            textDeviceName.visibility = View.VISIBLE // Make sure the TextView is visible
            // Set the device name, or a default message if the name is null or empty
            textDeviceName.text = deviceName.takeIf { !it.isNullOrEmpty() } ?: "Connected" //
            imageConnectionStatus.setImageResource(R.drawable.ic_bluetooth_connected)
            imageConnectionStatus.setColorFilter(Color.GREEN)
        } else {
            // Disconnected State
            textDeviceName.visibility = View.GONE // Hide the TextView when disconnected
            imageConnectionStatus.setImageResource(R.drawable.ic_bluetooth_disconnected)
            imageConnectionStatus.setColorFilter(Color.RED)
        }
    }
}
//