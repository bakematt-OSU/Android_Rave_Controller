package com.example.android_rave_controller.ui.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.android_rave_controller.R

class BluetoothFragment : Fragment() {

    private lateinit var scanButton: Button
    private lateinit var devicesListView: ListView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val devices = ArrayList<BluetoothDevice>()
    private val deviceNames = ArrayList<String>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (ActivityCompat.checkSelfPermission(
                                requireContext(),
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // Permissions are not granted, you should request them.
                            // For simplicity, this is not handled in this code snippet.
                            return
                        }
                        if (it.name != null && !devices.contains(it)) {
                            devices.add(it)
                            deviceNames.add(it.name)
                            deviceListAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bluetooth, container, false)
        scanButton = view.findViewById(R.id.scan_button)
        devicesListView = view.findViewById(R.id.devices_list_view)
        deviceListAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceNames)
        devicesListView.adapter = deviceListAdapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        scanButton.setOnClickListener {
            startScanning()
        }

        devicesListView.setOnItemClickListener { _, _, position, _ ->
            val device = devices[position]
            // Handle connection logic here
            Toast.makeText(requireContext(), "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(receiver, filter)

        return view
    }

    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            return
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        devices.clear()
        deviceNames.clear()
        deviceListAdapter.notifyDataSetChanged()
        bluetoothAdapter.startDiscovery()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions are not granted, you should request them.
            // For simplicity, this is not handled in this code snippet.
            return
        }
        bluetoothAdapter.cancelDiscovery()
        requireActivity().unregisterReceiver(receiver)
    }
}