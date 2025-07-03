package com.example.android_rave_controller

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

object BluetoothService {

    // --- UUIDs from your RP2040 Code ---
    private val LED_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    private val CMD_CHARACTERISTIC_UUID = UUID.fromString("00002a57-0000-1000-8000-00805f9b34fb")

    // --- Add LiveData for Connection State ---
    private val _connectionState = MutableLiveData(false)
    val connectionState: LiveData<Boolean> get() = _connectionState

    // --- Callback Interface ---
    interface ConnectionListener {
        fun onConnectionSuccess()
    }
    private var connectionListener: ConnectionListener? = null

    private var bluetoothGatt: BluetoothGatt? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null
    private var appContext: Context? = null // Store context for Toasts

    // This callback is where the app handles connection events and service discovery
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceName = if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.device.name ?: "Unknown Device"
            } else {
                "Unknown Device"
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.postValue(true)
                showToast("Connected to $deviceName! Discovering services...")
                if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.postValue(false)
                cmdCharacteristic = null
                showToast("Disconnected from $deviceName.")
                disconnect() // Clean up resources
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val deviceName = if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.device.name ?: "Unknown Device"
            } else {
                "Unknown Device"
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(LED_SERVICE_UUID)
                if (service != null) {
                    cmdCharacteristic = service.getCharacteristic(CMD_CHARACTERISTIC_UUID)
                    if (cmdCharacteristic != null) {
                        showToast("$deviceName is ready to send commands!")
                        // --- Notify the listener that we are ready ---
                        connectionListener?.onConnectionSuccess()
                    } else {
                        showToast("Command characteristic not found on $deviceName.")
                        disconnect()
                    }
                } else {
                    showToast("LED service not found on $deviceName.")
                    disconnect()
                }
            } else {
                showToast("Service discovery failed on $deviceName.")
                disconnect()
            }
        }
    }

    fun setConnectionListener(listener: ConnectionListener?) {
        this.connectionListener = listener
    }

    fun connect(context: Context, device: BluetoothDevice) {
        appContext = context.applicationContext // Store context for later use
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth connect permission not granted.")
            return
        }
        // Use the gattCallback to handle connection results
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun sendCommand(command: String) {
        // Check LiveData for connection state
        if (_connectionState.value != true || bluetoothGatt == null || cmdCharacteristic == null) {
            showToast("Cannot send command. Not connected.")
            return
        }

        val characteristic = cmdCharacteristic ?: return
        characteristic.value = command.toByteArray(Charsets.UTF_8)
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun disconnect() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _connectionState.postValue(false)
        }
    }

    // Helper to show toasts from anywhere
    private fun showToast(message: String) {
        appContext?.let {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}