package com.example.android_rave_controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectionViewModel : ViewModel() {

    // LiveData for the connection status (true/false)
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    // LiveData for the connected device's name
    private val _deviceName = MutableLiveData<String?>()
    val deviceName: LiveData<String?> = _deviceName

    // Function to update the status and device name together
    fun updateConnection(newStatus: Boolean, newDeviceName: String?) {
        _isConnected.value = newStatus
        _deviceName.value = newDeviceName
    }
}