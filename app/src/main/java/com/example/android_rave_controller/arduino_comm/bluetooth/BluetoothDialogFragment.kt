package com.example.android_rave_controller.arduino_comm.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
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
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_rave_controller.databinding.ActivityBluetoothBinding
import java.util.*

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

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB")))
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device != null && device.name != null && !devices.any { it.address == device.address }) {
                if (ActivityCompat.checkSelfPermission(
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
                startScanning()
            } else {
                Toast.makeText(
                    context,
                    "Permissions are required for Bluetooth functionality.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityBluetoothBinding.inflate(inflater, container, false)
        dialog?.setTitle("Scan for Devices")
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

        BluetoothService.connectionState.observe(viewLifecycleOwner) { isConnected ->
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
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@BluetoothDeviceAdapter
            }
            BluetoothService.connect(requireContext(), device)
            Toast.makeText(context, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
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
            ActivityCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(missingPermissions.toTypedArray())
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        devices.clear()
        deviceListAdapter.notifyDataSetChanged()
        bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Toast.makeText(context, "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (bleScanner == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
