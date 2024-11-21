package com.app.lockcompose

import ShowAppList
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.lockcompose.screens.DevicesScreen
import com.app.lockcompose.ui.theme.LockComposeTheme



class MainActivity : ComponentActivity() {

    // Register permission request handlers for each permission
    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                // After camera permission is granted, request Location permission
                requestLocationPermission()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                // After location permission is granted, request Nearby Devices permission
                requestNearbyDevicesPermission()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val nearbyDevicesPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Nearby Devices permission granted", Toast.LENGTH_SHORT).show()
                // After Nearby Devices permission is granted, request Bluetooth Connect permission
                requestBluetoothConnectPermission()
            } else {
                Toast.makeText(this, "Nearby Devices permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val bluetoothConnectPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Bluetooth Connect permission granted", Toast.LENGTH_SHORT).show()
                // After Bluetooth Connect permission is granted, enable location if needed
                enableLocationIfNeeded()
            } else {
                Toast.makeText(this, "Bluetooth Connect permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start by requesting Camera permission
        requestCameraPermission()

        setContent {
            LockComposeTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "showAppList") {
                    composable("showAppList") { ShowAppList(navController) }
                    composable("devices") { DevicesScreen() }
                }
            }
        }
    }

    // Function to request Camera permission
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        } else {
            // If Camera permission is already granted, request Location permission
            requestLocationPermission()
        }
    }

    // Function to request Location permission
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // If Location permission is already granted, request Nearby Devices permission
            requestNearbyDevicesPermission()
        }
    }

    // Function to request Nearby Devices permission (if required)
    private fun requestNearbyDevicesPermission() {
        // Check for Nearby Devices permission (for example, Nearby Devices API or Bluetooth permissions)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) {
            nearbyDevicesPermissionRequest.launch(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // If Nearby Devices permission is already granted, request Bluetooth Connect permission
            requestBluetoothConnectPermission()
        }
    }

    // Function to request Bluetooth Connect permission
    private fun requestBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            bluetoothConnectPermissionRequest.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // If Bluetooth Connect permission is already granted, enable location if needed
            enableLocationIfNeeded()
        }
    }

    private fun enableLocationIfNeeded() {
        // Get the system LocationManager service
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Check if location services are enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location is disabled, prompt the user to enable it
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, REQUEST_ENABLE_LOCATION)
        }
    }

    companion object {
        const val REQUEST_ENABLE_LOCATION = 1001
    }
}

