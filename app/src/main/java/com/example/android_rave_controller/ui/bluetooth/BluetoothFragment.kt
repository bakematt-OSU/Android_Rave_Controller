package com.example.android_rave_controller.ui.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.android_rave_controller.BluetoothService
import com.example.android_rave_controller.R
import java.util.*

class BluetoothFragment : Fragment(), BluetoothService.ConnectionListener {

    private lateinit var scanButton: Button
    private lateinit var devicesListView: ListView
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val devices = ArrayList<BluetoothDevice>()
    private val deviceNames = ArrayList<String>()

    private val bleScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // --- This filter looks for devices advertising your specific service UUID ---
    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")))
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            if (result.device != null && result.device.name != null && !devices.contains(result.device)) {
                devices.add(result.device)
                deviceNames.add(result.device.name)
                deviceListAdapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(requireContext(), "BLE Scan Failed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

    // Permission launcher
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                startScanning()
            } else {
                Toast.makeText(requireContext(), "Permissions are required for Bluetooth functionality.", Toast.LENGTH_LONG).show()
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

        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show()
            scanButton.isEnabled = false
        }

        scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        devicesListView.setOnItemClickListener { _, _, position, _ ->
            stopScanning()
            val device = devices[position]
            BluetoothService.connect(requireContext(), device)
            Toast.makeText(requireContext(), "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
        }

        // --- Register this fragment as the listener ---
        BluetoothService.setConnectionListener(this)

        return view
    }

    override fun onConnectionSuccess() {
        // --- This will be called from the BluetoothService ---
        // We need to make sure we do UI navigation on the main thread
        Handler(Looper.getMainLooper()).post {
            findNavController().navigate(R.id.navigation_home)
        }
    }

    private fun checkPermissionsAndScan() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(missingPermissions.toTypedArray())
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        devices.clear()
        deviceNames.clear()
        deviceListAdapter.notifyDataSetChanged()
        bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Toast.makeText(requireContext(), "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        bleScanner?.stopScan(scanCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop scanning and unregister the listener to prevent memory leaks
        stopScanning()
        BluetoothService.setConnectionListener(null)
    }
}