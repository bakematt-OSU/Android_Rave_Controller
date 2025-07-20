package com.example.android_rave_controller

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.arduino_comm_ble.ConnectionViewModel
import com.example.android_rave_controller.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private lateinit var toolbarManager: ToolbarManager

    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_segments, R.id.navigation_device, R.id.navigation_configurations
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize BluetoothService once for the app's lifecycle.
        BluetoothService.initialize(applicationContext)

        // Setup ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup Navigation
        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Delegate toolbar management
        toolbarManager = ToolbarManager(this, binding.toolbar, navController, connectionViewModel)
        toolbarManager.initialize()

        // CORRECTED OBSERVERS
        // This observer reacts to changes in connection status
        BluetoothService.connectionState.observe(this) { isConnected ->
            connectionViewModel.updateConnection(isConnected, BluetoothService.connectedDeviceName.value)
        }

        // This observer reacts to changes in the device name
        BluetoothService.connectedDeviceName.observe(this) { deviceName ->
            // Update the connection view model only if currently connected
            if (connectionViewModel.connectionStatus.value?.isConnected == true) {
                connectionViewModel.updateConnection(true, deviceName)
            }
        }
    }
}