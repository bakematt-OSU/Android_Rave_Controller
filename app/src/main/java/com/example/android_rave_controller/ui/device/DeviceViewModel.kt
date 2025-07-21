package com.example.android_rave_controller.ui.device

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.android_rave_controller.arduino_comm_ble.BluetoothService
import com.example.android_rave_controller.arduino_comm_ble.control.DeviceProtocolHandler

class DeviceViewModel(application: Application) : AndroidViewModel(application) {
    // Instantiate DeviceProtocolHandler here
    val deviceProtocolHandler = BluetoothService.deviceProtocolHandler
}