// src/main/java/com/example/android_rave_controller/arduino_comm_ble/BluetoothService.kt
package com.example.android_rave_controller.arduino_comm_ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.UUID

object BluetoothService {

    private const val TAG = "BluetoothService"

    // --- CORRECT UUIDs to match your RP2040 Firmware's BLEManager.h ---
    private val LED_SERVICE_UUID = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
    private val RX_CHARACTERISTIC_UUID = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214") // For sending commands to Arduino (App -> Arduino)
    private val TX_CHARACTERISTIC_UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214") // For receiving notifications from Arduino (Arduino -> App)
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Standard Client Characteristic Configuration Descriptor

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableLiveData(false)
    val connectionState: LiveData<Boolean> get() = _connectionState

    // To store the name of the currently connected device
    var connectedDeviceName: String? = null

    private lateinit var applicationContext: Context // Needs to be initialized

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        // Initialize DeviceProtocolHandler here
        DeviceProtocolHandler.initialize(applicationContext)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.postValue(true)
                bluetoothGatt = gatt // Store the gatt object
                // The connectedDeviceName is now set by the connect function itself,
                // so we don't re-read gatt.device.name here.
                Log.i(TAG, "Connected to GATT server. Discovering services...")
                if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showToast("Bluetooth permissions not granted for service discovery.")
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.")
                cleanupConnection()
            }
        }

        // Corrected override signature for onServicesDiscovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(LED_SERVICE_UUID)
                if (service == null) {
                    Log.e(TAG, "LED service not found: $LED_SERVICE_UUID")
                    showToast("LED service not found on device.")
                    cleanupConnection()
                    return
                }

                rxCharacteristic = service.getCharacteristic(RX_CHARACTERISTIC_UUID)
                txCharacteristic = service.getCharacteristic(TX_CHARACTERISTIC_UUID)

                if (rxCharacteristic == null || txCharacteristic == null) {
                    Log.e(TAG, "Required characteristics not found. RX: $RX_CHARACTERISTIC_UUID, TX: $TX_CHARACTERISTIC_UUID")
                    showToast("Required BLE characteristics not found.")
                    cleanupConnection()
                    return
                }

                // Enable notifications on the TX characteristic
                enableNotifications(gatt, txCharacteristic!!)
            } else {
                Log.e(TAG, "onServicesDiscovered received status: $status")
                showToast("Service discovery failed: $status")
                cleanupConnection()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic.uuid == rxCharacteristic?.uuid) { // Check if it's our RX characteristic
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Write successful. Notifying handler to send next command.")
                    // Only signal command sent if the write was successful
                    DeviceProtocolHandler.onCommandSent()
                } else {
                    Log.e(TAG, "Write failed with status: $status")
                    showToast("BLE write failed: $status")
                    // If write fails, we should still try to send the next command, but maybe with a delay or retry logic
                    // For now, let's still signal onCommandSent to keep the queue moving, but logging the error.
                    // A more robust solution would involve retries or explicit error handling in DeviceProtocolHandler.
                    DeviceProtocolHandler.onCommandSent() // Still move the queue forward to avoid deadlock
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == txCharacteristic?.uuid) { // Check if it's our TX characteristic
                DeviceProtocolHandler.parseResponse(value)
            } else {
                Log.w(TAG, "Received notification from unknown characteristic: ${characteristic.uuid}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful: ${descriptor.uuid}")
                showToast("Connected to Rave Controller!")
                // After successful connection and notification setup, request device status
                DeviceProtocolHandler.requestDeviceStatus() // Trigger the initial status request
            } else {
                Log.e(TAG, "Descriptor write failed: $status")
                showToast("Failed to enable notifications.")
                cleanupConnection()
            }
        }
    }

    // Connect function now takes BluetoothDevice and initialDeviceName
    fun connect(context: Context, device: BluetoothDevice, initialDeviceName: String?) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permissions not granted for connection.")
            return
        }
        connectedDeviceName = initialDeviceName // Set the name immediately upon initiating connection
        // This will trigger onConnectionStateChange in gattCallback
        device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        cleanupConnection()
    }

    private fun cleanupConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
        connectedDeviceName = null // Clear the connected device name
        _connectionState.postValue(false)
    }

    fun sendCommand(bytes: ByteArray) {
        if (_connectionState.value != true || bluetoothGatt == null || rxCharacteristic == null) {
            showToast("Cannot send command. Not connected or characteristic not found.")
            return
        }

        val characteristic = rxCharacteristic ?: run {
            showToast("RX characteristic not found.")
            return
        }

        if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permissions not granted.")
            return
        }

        characteristic.value = bytes
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT // Or WRITE_TYPE_NO_RESPONSE

        bluetoothGatt?.let { gatt ->
            val result = gatt.writeCharacteristic(characteristic)
            if (!result) {
                Log.e(TAG, "Failed to write characteristic immediately (might be queued or rejected).")
                // Do NOT call onCommandSent here, as it implies the command was successfully sent
                // and the queue can move. The onCharacteristicWrite callback will handle this.
            } else {
                Log.d(TAG, "Command write initiated to BLE: ${bytes.joinToString(prefix = "0x") { "%02X".format(it) }}")
                // Do NOT call onCommandSent here either. Wait for the callback.
            }
        } ?: showToast("BluetoothGatt is null.")
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth permissions not granted.")
            return
        }

        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.e(TAG, "CCCD descriptor not found for characteristic: ${characteristic.uuid}")
            showToast("Failed to find notification descriptor.")
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val result = gatt.writeDescriptor(descriptor)
        if (!result) {
            Log.e(TAG, "Failed to write descriptor to enable notifications.")
            showToast("Failed to enable BLE notifications.")
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}