package com.example.android_rave_controller

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.android_rave_controller.arduino_comm_ble.BluetoothDialogFragment
import com.example.android_rave_controller.arduino_comm_ble.ConnectionViewModel

class ToolbarManager(
    private val activity: AppCompatActivity,
    private val toolbar: androidx.appcompat.widget.Toolbar,
    private val navController: NavController,
    private val connectionViewModel: ConnectionViewModel
) {

    private val toolbarTitle: TextView = toolbar.findViewById(R.id.toolbar_title)
    private val textDeviceName: TextView = toolbar.findViewById(R.id.text_device_name)
    private val imageConnectionStatus: ImageView = toolbar.findViewById(R.id.image_connection_status)
    private val statusLayout: LinearLayout = toolbar.findViewById(R.id.status_layout)

    fun initialize() {
        setupNavigationListener()
        setupConnectionStatusObserver()
        setupClickListener()
        updateConnectionStatus(false, null) // Set initial state
    }

    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbarTitle.text = destination.label
        }
    }

    private fun setupConnectionStatusObserver() {
        connectionViewModel.connectionStatus.observe(activity as LifecycleOwner) { status ->
            updateConnectionStatus(status.isConnected, status.deviceName)
        }
    }

    private fun setupClickListener() {
        statusLayout.setOnClickListener {
            BluetoothDialogFragment().show(activity.supportFragmentManager, "BluetoothDialog")
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            textDeviceName.visibility = View.VISIBLE
            textDeviceName.text = deviceName.takeIf { !it.isNullOrEmpty() } ?: "Connected"
            imageConnectionStatus.setImageResource(R.drawable.ic_bluetooth_connected)
            imageConnectionStatus.setColorFilter(Color.GREEN)
        } else {
            textDeviceName.visibility = View.GONE
            imageConnectionStatus.setImageResource(R.drawable.ic_bluetooth_disconnected)
            imageConnectionStatus.setColorFilter(Color.RED)
        }
    }
}