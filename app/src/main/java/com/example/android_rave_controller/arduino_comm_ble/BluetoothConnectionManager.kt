// In main/java/com/example/android_rave_controller/arduino_comm_ble/BluetoothConnectionManager.kt

package com.example.android_rave_controller.arduino_comm_ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android_rave_controller.arduino_comm_ble.control.DeviceProtocolHandler
import com.example.android_rave_controller.arduino_comm_ble.control.LedControllerCommands
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothConnectionManager(
    private val context: Context,
    private val deviceProtocolHandler: DeviceProtocolHandler
) {
    private val TAG = "BluetoothConnectionManager"

    private var connectingDeviceName: String? = null

    // Removed heartbeatRunnable and HEARTBEAT_INTERVAL_MS as requested.
    // The 'handler' and its imports are also removed since they were only
    // used for heartbeat functionality.

    // UUIDs remain the same
    private val LED_SERVICE_UUID = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
    private val RX_CHARACTERISTIC_UUID = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214")
    private val TX_CHARACTERISTIC_UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null
    var rxCharacteristic: BluetoothGattCharacteristic? = null
    var txCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionStatus = MutableLiveData(ConnectionStatus(false, null))
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val gattCallback = GattCallbackHandler(this, deviceProtocolHandler)


    fun connect(device: BluetoothDevice) {
        connectingDeviceName = device.name ?: device.address
        _connectionStatus.postValue(ConnectionStatus(false, connectingDeviceName))
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }


    fun disconnect() {
        bluetoothGatt?.disconnect()
        cleanup()
    }

    private fun cleanup() {
        // Removed handler.removeCallbacks(heartbeatRunnable) as heartbeat functionality is removed.
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
        connectingDeviceName = null
        _connectionStatus.postValue(ConnectionStatus(false, null))
    }


    fun onDeviceConnected(gatt: BluetoothGatt) {
        _connectionStatus.postValue(ConnectionStatus(true, connectingDeviceName))
        bluetoothGatt = gatt
    }

    fun onDeviceDisconnected() {
        cleanup()
    }

    fun onServicesDiscovered(gatt: BluetoothGatt) {
        val service = gatt.getService(LED_SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "Service not found")
            disconnect()
            return
        }
        txCharacteristic = service.getCharacteristic(TX_CHARACTERISTIC_UUID)
        rxCharacteristic = service.getCharacteristic(RX_CHARACTERISTIC_UUID)
        if (txCharacteristic == null || rxCharacteristic == null) {
            Log.e(TAG, "Characteristics not found")
            disconnect()
            return
        }
        enableNotifications(txCharacteristic!!)
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        val cccd = characteristic.getDescriptor(CCCD_UUID)
        if (cccd == null) {
            Log.e(TAG, "CCCD not found for characteristic ${characteristic.uuid}")
            return
        }

        bluetoothGatt?.setCharacteristicNotification(characteristic, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothGatt?.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            bluetoothGatt?.writeDescriptor(cccd)
        }
    }

    /**
     * Sends a command (byte array) to the Arduino via the RX characteristic.
     * This function is called by the CommandQueue.
     * @param data The byte array representing the command to send.
     */
    fun sendCommand(data: ByteArray) {
        if (rxCharacteristic != null && bluetoothGatt != null) {
            // Log the actual bytes being sent for debugging
            Log.d(TAG, "Attempting to send command: ${data.joinToString(separator = " ") { String.format("%02X", it) }} (${data.size} bytes)")

            // Set the value of the characteristic
            rxCharacteristic!!.value = data

            // Determine the write type based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13 (API 33) and above, use the new writeCharacteristic method
                bluetoothGatt?.writeCharacteristic(rxCharacteristic!!, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                // For older Android versions, set writeType and then call writeCharacteristic
                @Suppress("DEPRECATION")
                rxCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                bluetoothGatt!!.writeCharacteristic(rxCharacteristic!!)
            }
        } else {
            Log.w(TAG, "Cannot send command, rxCharacteristic or bluetoothGatt is null. Is device connected?")
        }
    }
}
