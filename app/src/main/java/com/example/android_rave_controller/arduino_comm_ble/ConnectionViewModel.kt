// In main/java/com/example/android_rave_controller/arduino_comm_ble/ConnectionViewModel.kt
package com.example.android_rave_controller.arduino_comm_ble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel

data class ConnectionStatus(val isConnected: Boolean, val deviceName: String?)

class ConnectionViewModel : ViewModel() {

    private val _connectionStatus = MediatorLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    init {
        // This will automatically update the ViewModel whenever the service's status changes
        _connectionStatus.addSource(BluetoothService.connectionStatus) { newStatus ->
            _connectionStatus.value = newStatus
        }
    }
}