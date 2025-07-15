package com.example.android_rave_controller.arduino_comm.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_rave_controller.databinding.ActivityBluetoothBinding
import java.util.*

class BluetoothActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothBinding

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private lateinit var deviceListAdapter: BluetoothDeviceAdapter
    private val devices = ArrayList<BluetoothDevice>()
    private var selectedDeviceName: String? = null

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // Scan filter updated to use the "180A" service UUID.
    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")))
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device != null && device.name != null && !devices.any { it.address == device.address }) {
                if (ActivityCompat.checkSelfPermission(this@BluetoothActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                devices.add(device)
                deviceListAdapter.notifyItemInserted(devices.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(this@BluetoothActivity, "BLE Scan Failed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                startScanning()
            } else {
                Toast.makeText(this, "Permissions are required for Bluetooth functionality.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The manual window layout adjustment has been removed.
        // The theme specified in AndroidManifest.xml will handle the dialog appearance.

        // Setup RecyclerView
        setupRecyclerView()


        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show()
            binding.scanButton.isEnabled = false
        }

        binding.scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        // Observe the connection state from BluetoothService
        BluetoothService.connectionState.observe(this) { isConnected ->
            if (isConnected) {
                val resultIntent = Intent().apply {
                    putExtra("deviceName", selectedDeviceName)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        deviceListAdapter = BluetoothDeviceAdapter(devices) { device ->
            // This is the click listener lambda
            stopScanning()
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth connect permission not granted.", Toast.LENGTH_SHORT).show()
                return@BluetoothDeviceAdapter
            }
            selectedDeviceName = device.name
            BluetoothService.connect(this, device)
            Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
        }
        binding.devicesRecyclerView.adapter = deviceListAdapter
        binding.devicesRecyclerView.layoutManager = LinearLayoutManager(this)
    }


    private fun checkPermissionsAndScan() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(missingPermissions.toTypedArray())
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth scan permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }
        devices.clear()
        deviceListAdapter.notifyDataSetChanged()
        bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth scan permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }
        bleScanner?.stopScan(scanCallback)
    }

    // The call to stopScanning() has been removed from onDestroy() to prevent the race condition.
    override fun onDestroy() {
        super.onDestroy()
    }
}
