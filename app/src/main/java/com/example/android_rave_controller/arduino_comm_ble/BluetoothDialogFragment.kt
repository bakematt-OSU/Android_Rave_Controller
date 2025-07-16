// src/main/java/com/example/android_rave_controller/arduino_comm_ble/BluetoothDialogFragment.kt
package com.example.android_rave_controller.arduino_comm_ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_rave_controller.databinding.ActivityBluetoothBinding
import java.util.*
import android.app.Activity // Import android.app.Activity

class BluetoothDialogFragment : DialogFragment() {

    private var _binding: ActivityBluetoothBinding? = null
    private val binding get() = _binding!!

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private lateinit var deviceListAdapter: BluetoothDeviceAdapter
    private val devices = ArrayList<BluetoothDevice>()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // CORRECTED: Use the LED_SERVICE_UUID from your Arduino firmware
    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214")))
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // Allow devices without a name to be listed, but still filter for unique addresses
            if (device != null && !devices.any { it.address == device.address }) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                devices.add(device)
                // Ensure UI updates are on the main thread
                activity?.runOnUiThread {
                    deviceListAdapter.notifyItemInserted(devices.size - 1)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(context, "BLE Scan Failed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                // Permissions granted, now check if Bluetooth is enabled
                if (bluetoothAdapter?.isEnabled == true) {
                    startScanning()
                } else {
                    promptEnableBluetooth()
                }
            } else {
                Toast.makeText(
                    context,
                    "Permissions are required for Bluetooth functionality.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // CORRECTED: Use android.app.Activity.RESULT_OK
        if (result.resultCode == Activity.RESULT_OK) {
            startScanning()
        } else {
            Toast.makeText(context, "Bluetooth must be enabled to scan for devices.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityBluetoothBinding.inflate(inflater, container, false)
        dialog?.setTitle("Scan for Devices")

        // Initialize BluetoothService here with application context
        BluetoothService.initialize(requireContext().applicationContext)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG)
                .show()
            binding.scanButton.isEnabled = false
        }

        binding.scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        BluetoothService.connectionState.observe(viewLifecycleOwner) { isConnected: Boolean ->
            if (isConnected) {
                dismiss() // Close the dialog on successful connection
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.75).toInt()
            window.setLayout(width, height)
        }
    }

    private fun setupRecyclerView() {
        deviceListAdapter = BluetoothDeviceAdapter(devices) { device ->
            stopScanning()
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Bluetooth connect permission not granted.", Toast.LENGTH_SHORT).show()
                return@BluetoothDeviceAdapter
            }
            BluetoothService.connect(requireContext(), device, device.name) // Pass device.name here
            Toast.makeText(context, "Connecting to ${device.name ?: device.address}", Toast.LENGTH_SHORT).show()
        }
        binding.devicesRecyclerView.adapter = deviceListAdapter
        binding.devicesRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun checkPermissionsAndScan() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(missingPermissions.toTypedArray())
        } else {
            // Permissions are granted, now check if Bluetooth is enabled
            if (bluetoothAdapter?.isEnabled == true) {
                startScanning()
            } else {
                promptEnableBluetooth()
            }
        }
    }

    private fun promptEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(context, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Bluetooth scan permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }
        devices.clear()
        deviceListAdapter.notifyDataSetChanged()
        bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Toast.makeText(context, "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (bleScanner == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // No Toast here, as it might be called on fragment destruction
            return
        }
        bleScanner?.stopScan(scanCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScanning()
        _binding = null
    }
}