package com.example.android_rave_controller.ui.bluetooth

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
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.android_rave_controller.BluetoothService
import com.example.android_rave_controller.R
import com.example.android_rave_controller.databinding.FragmentBluetoothBinding
import java.util.UUID

class BluetoothFragment : Fragment(), BluetoothService.ConnectionListener {

    private var _binding: FragmentBluetoothBinding? = null
    private val binding get() = _binding!!

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        // The typo has been corrected in the line below
        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val devices = ArrayList<BluetoothDevice>()
    private val deviceNames = ArrayList<String>()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")))
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device != null && device.name != null && !devices.contains(device)) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                devices.add(device)
                deviceNames.add(device.name)
                deviceListAdapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(requireContext(), "BLE Scan Failed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

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
    ): View {
        _binding = FragmentBluetoothBinding.inflate(inflater, container, false)

        deviceListAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceNames)
        binding.devicesListView.adapter = deviceListAdapter

        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth is not supported on this device.", Toast.LENGTH_LONG).show()
            binding.scanButton.isEnabled = false
        }

        binding.scanButton.setOnClickListener {
            checkPermissionsAndScan()
        }

        binding.devicesListView.setOnItemClickListener { _, _, position, _ ->
            stopScanning()
            val device = devices[position]
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Bluetooth connect permission not granted.", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }
            BluetoothService.connect(requireContext(), device)
            Toast.makeText(requireContext(), "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
        }

        BluetoothService.setConnectionListener(this)

        return binding.root
    }

    override fun onConnectionSuccess() {
        Handler(Looper.getMainLooper()).post {
            findNavController().navigate(R.id.action_bluetooth_to_dashboard)
        }
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
            ActivityCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(missingPermissions.toTypedArray())
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Bluetooth scan permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }
        devices.clear()
        deviceNames.clear()
        deviceListAdapter.notifyDataSetChanged()
        bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Toast.makeText(requireContext(), "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Bluetooth scan permission not granted.", Toast.LENGTH_SHORT).show()
            return
        }
        bleScanner?.stopScan(scanCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopScanning()
        BluetoothService.setConnectionListener(null)
        _binding = null
    }
}