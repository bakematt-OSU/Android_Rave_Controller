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
import com.example.android_rave_controller.arduino_comm_ble.control.CommandGetters
import com.example.android_rave_controller.arduino_comm_ble.control.DeviceProtocolHandler
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothConnectionManager(
    private val context: Context,
    private val deviceProtocolHandler: DeviceProtocolHandler
) {
    private val TAG = "BluetoothConnectionManager"

    // UUIDs remain the same
    private val LED_SERVICE_UUID = UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")
    private val RX_CHARACTERISTIC_UUID = UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214")
    private val TX_CHARACTERISTIC_UUID = UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214")
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null
    var rxCharacteristic: BluetoothGattCharacteristic? = null
    var txCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableLiveData<Boolean>(false)
    val connectionState: LiveData<Boolean> = _connectionState

    private val _deviceName = MutableLiveData<String?>(null)
    val deviceName: LiveData<String?> = _deviceName

    private val gattCallback = GattCallbackHandler(this, deviceProtocolHandler)

    fun connect(device: BluetoothDevice) {
        _deviceName.postValue(device.name ?: device.address)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        cleanup()
    }

    private fun cleanup() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
        _connectionState.postValue(false)
        _deviceName.postValue(null)
    }

    fun onDeviceConnected(gatt: BluetoothGatt) {
        _connectionState.postValue(true)
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
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(cccd)
        }

        // After notifications are enabled, the device is ready.
        // Request the effects now.
//        CommandGetters.requestAllEffects()
    }

    fun sendCommand(data: ByteArray) {
        if (rxCharacteristic != null && bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(rxCharacteristic!!, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                rxCharacteristic!!.value = data
                rxCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                bluetoothGatt!!.writeCharacteristic(rxCharacteristic!!)
            }
        } else {
            Log.w(TAG, "Cannot send command, characteristic or gatt is null")
        }
    }
}