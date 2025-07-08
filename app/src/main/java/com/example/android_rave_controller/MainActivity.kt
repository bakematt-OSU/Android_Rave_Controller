package com.example.android_rave_controller

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.android_rave_controller.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val connectionViewModel: ConnectionViewModel by viewModels()

    // Declare properties to hold the views from the toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var textDeviceName: TextView
    private lateinit var imageConnectionStatus: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Find the views inside the toolbar manually
        toolbarTitle = binding.toolbar.findViewById(R.id.toolbar_title)
        textDeviceName = binding.toolbar.findViewById(R.id.text_device_name)
        imageConnectionStatus = binding.toolbar.findViewById(R.id.image_connection_status)

        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_segments)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Listen for screen changes to update the title
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbarTitle.text = destination.label
        }

        // Observe the BluetoothService directly to update the ViewModel and the UI
        BluetoothService.connectionState.observe(this) { isConnected ->
            val deviceName = if (isConnected) {
                BluetoothService.connectedDeviceName
            } else {
                null
            }
            connectionViewModel.updateConnection(isConnected, deviceName)
            updateConnectionStatus(isConnected, deviceName) // Directly update the UI here
        }

        // Set initial state
        updateConnectionStatus(false, null)
    }

    private fun updateConnectionStatus(isConnected: Boolean, deviceName: String?) {
        if (isConnected && deviceName != null) {
            // Connected State
            textDeviceName.visibility = View.VISIBLE
            textDeviceName.text = deviceName
            imageConnectionStatus.setImageResource(R.drawable.ic_bluetooth_connected)
            imageConnectionStatus.setColorFilter(Color.GREEN)
        } else {
            // Disconnected State
            textDeviceName.visibility = View.GONE
            imageConnectionStatus.setImageResource(R.drawable.ic_bluetooth_disconnected)
            imageConnectionStatus.setColorFilter(Color.RED)
        }
    }
}