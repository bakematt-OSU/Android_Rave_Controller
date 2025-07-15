package com.example.android_rave_controller.arduino_comm.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.android_rave_controller.databinding.ListItemBluetoothDeviceBinding

class BluetoothDeviceAdapter(
    private val devices: List<BluetoothDevice>,
    private val onItemClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(private val binding: ListItemBluetoothDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BluetoothDevice, onItemClick: (BluetoothDevice) -> Unit) {
            if (ActivityCompat.checkSelfPermission(
                    itemView.context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If permission is not granted, you might want to show a default text
                // or handle it in a way that doesn't crash the app.
                // For now, we'll just return, but a real app should handle this more gracefully.
                return
            }
            binding.deviceNameTextView.text = device.name ?: "Unknown Device"
            binding.deviceAddressTextView.text = device.address
            itemView.setOnClickListener { onItemClick(device) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ListItemBluetoothDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position], onItemClick)
    }

    override fun getItemCount(): Int = devices.size
}
