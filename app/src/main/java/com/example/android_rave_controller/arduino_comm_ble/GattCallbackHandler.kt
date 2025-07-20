package com.example.android_rave_controller.arduino_comm_ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
import com.example.android_rave_controller.arduino_comm_ble.control.CommandGetters
import com.example.android_rave_controller.arduino_comm_ble.control.DeviceProtocolHandler
import java.util.UUID

@SuppressLint("MissingPermission")
class GattCallbackHandler(
    private val connectionManager: BluetoothConnectionManager,
    private val deviceProtocolHandler: DeviceProtocolHandler
) : BluetoothGattCallback() {

    private val TAG = "GattCallbackHandler"
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            connectionManager.onDeviceConnected(gatt)
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.i(TAG, "Disconnected from GATT server.")
            connectionManager.onDeviceDisconnected()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            connectionManager.onServicesDiscovered(gatt)
        } else {
            Log.w(TAG, "onServicesDiscovered received: $status")
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Write successful for characteristic: ${characteristic.uuid}")
            deviceProtocolHandler.onCommandSent()
        } else {
            Log.e(TAG, "Write failed with status: $status")
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        deviceProtocolHandler.parseResponse(value)
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Descriptor write successful.")
            // ADD THIS LINE
            CommandGetters.requestAllEffects()
        } else {
            Log.e(TAG, "Descriptor write failed with status: $status")
        }
    }
}