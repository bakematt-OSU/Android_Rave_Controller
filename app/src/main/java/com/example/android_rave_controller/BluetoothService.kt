package com.example.android_rave_controller

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.*

object BluetoothService {

    private const val TAG = "BluetoothService"
    // --- UUIDs to match your RP2040 Firmware ---
    private val LED_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")
    private val CMD_CHARACTERISTIC_UUID = UUID.fromString("00002A57-0000-1000-8000-00805F9B34FB")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val _connectionState = MutableLiveData(false)
    val connectionState: LiveData<Boolean> get() = _connectionState
    var connectedDevice: BluetoothDevice? = null
    var connectedDeviceName: String? = null

    private var bluetoothGatt: BluetoothGatt? = null
    private var cmdCharacteristic: BluetoothGattCharacteristic? = null
    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceName = getDeviceName(gatt.device)

            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connectedDevice = gatt.device
                connectedDeviceName = deviceName
                _connectionState.postValue(true)
                showToast("Connected to $deviceName! Discovering services...")
                mainHandler.post {
                    if (!gatt.discoverServices()) {
                        Log.e(TAG, "discoverServices() failed to initiate.")
                        cleanupConnection()
                    }
                }
                return
            }

            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "Connection state error. Status: $status, NewState: $newState")
                cleanupConnection()
                showToast("Disconnected from $deviceName.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered successfully.")
                val service = gatt.getService(LED_SERVICE_UUID)
                if (service == null) {
                    Log.e(TAG, "LED service not found.")
                    cleanupConnection()
                    return
                }
                cmdCharacteristic = service.getCharacteristic(CMD_CHARACTERISTIC_UUID)
                if (cmdCharacteristic == null) {
                    Log.e(TAG, "Command characteristic not found.")
                    cleanupConnection()
                    return
                }
                enableNotifications(gatt, cmdCharacteristic!!)

            } else {
                Log.w(TAG, "onServicesDiscovered received failure status: $status")
                cleanupConnection()
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Notifications enabled successfully. Connection is ready.")
            } else {
                Log.e(TAG, "Failed to write descriptor, status: $status")
                cleanupConnection()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful. Notifying handler to send next command.")
                // THIS IS THE KEY FIX: The handler is now the only thing that advances the queue.
                DeviceProtocolHandler.onCommandSent()
            } else {
                Log.e(TAG, "Write failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            DeviceProtocolHandler.parseResponse(value)
        }

        @Deprecated("Use onCharacteristicChanged with byte array")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onCharacteristicChanged(gatt, characteristic, characteristic.value)
            }
        }
    }

    private fun cleanupConnection() {
        if (bluetoothGatt != null) {
            if (appContext != null && ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Cleaning up existing GATT connection.")
                refreshDeviceCache(bluetoothGatt)
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
            }
            bluetoothGatt = null
            cmdCharacteristic = null
            connectedDevice = null
            connectedDeviceName = null
            _connectionState.postValue(false)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Manual disconnect called.")
        cleanupConnection()
    }

    fun connect(context: Context, device: BluetoothDevice) {
        appContext = context.applicationContext

        if (bluetoothGatt != null) {
            Log.d(TAG, "A new connection was requested. Cleaning up the old one first.")
            cleanupConnection()
        }

        if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Bluetooth connect permission not granted.")
            return
        }

        Log.d(TAG, "Attempting to connect to ${getDeviceName(device)}")

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(appContext, false, gattCallback)
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val cccd = characteristic.getDescriptor(CCCD_UUID)
        if (cccd == null) {
            Log.e(TAG, "CCCD not found for characteristic ${characteristic.uuid}")
            cleanupConnection()
            return
        }

        if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { return }

        gatt.setCharacteristicNotification(characteristic, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(cccd)
            }
        }
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt?): Boolean {
        if (gatt == null) return false
        try {
            val refreshMethod = gatt.javaClass.getMethod("refresh")
            val success = refreshMethod.invoke(gatt) as Boolean
            Log.d(TAG, "Bluetooth cache refresh ${if (success) "succeeded" else "failed"}.")
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Could not refresh Bluetooth cache!", e)
        }
        return false
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return if (appContext != null && ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            device.name ?: "Unknown Device"
        } else {
            "Unknown Device"
        }
    }

    private fun showToast(message: String) {
        mainHandler.post {
            appContext?.let {
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun sendCommand(bytes: ByteArray) {
        if (_connectionState.value != true || bluetoothGatt == null || cmdCharacteristic == null) {
            showToast("Cannot send command. Not connected.")
            return
        }
        val characteristic = cmdCharacteristic ?: return
        if (ActivityCompat.checkSelfPermission(appContext!!, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt?.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = bytes
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
        }
        Log.d(TAG, "Sent bytes: ${bytes.joinToString { "0x%02X".format(it) }}")
    }
}