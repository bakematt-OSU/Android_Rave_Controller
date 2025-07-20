package com.example.android_rave_controller.arduino_comm_ble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class ConnectionStatus(val isConnected: Boolean, val deviceName: String?)

class ConnectionViewModel : ViewModel() {

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    fun updateConnection(newStatus: Boolean, newDeviceName: String?) {
        _connectionStatus.value = ConnectionStatus(newStatus, newDeviceName)
    }
}