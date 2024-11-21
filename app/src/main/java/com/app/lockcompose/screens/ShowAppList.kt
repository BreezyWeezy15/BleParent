import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAppList(navController: NavController) {
    val context = LocalContext.current

    val isLightTheme = !isSystemInDarkTheme()
    val allApps = remember { getInstalledApps(context) }
    val availableApps by remember { mutableStateOf(allApps.toMutableList()) }
    val selectedApps = remember { mutableStateListOf<InstalledApp>() }

    var expanded by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("Select Interval") }
    val timeIntervals = listOf("1 min", "15 min", "30 min", "45 min", "60 min", "75 min", "90 min", "120 min")
    val showDevicesDialog = remember { mutableStateOf(false) } // State to control device dialog
    var pinCode by remember { mutableStateOf("") }
    var devicesName by remember { mutableStateOf("") }
    fun parseInterval(interval: String): Int {
        return interval.replace(" min", "").toIntOrNull() ?: 0
    }

    var scannedData by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intentResult = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        if (intentResult != null && intentResult.contents != null) {
            scannedData = intentResult.contents
            Toast.makeText(context, "QR Code Scanned: $scannedData", Toast.LENGTH_SHORT).show()
            scanDevices(context,scannedData)
        } else {
            Toast.makeText(context, "Scan failed or canceled", Toast.LENGTH_SHORT).show()
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("》Rules list") },
                actions = {
                    IconButton(onClick = {
                        if (availableApps.isEmpty()) {
                            Toast.makeText(context, "No apps to delete", Toast.LENGTH_SHORT).show()
                        } else {
                            deleteAllAppsFromFirebase(context)
                            availableApps.clear()  // Clear the list locally
                        }
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Delete Apps"
                        )
                    }
                    // Add a new IconButton for navigating to the DevicesScreen
                    IconButton(onClick = { showDevicesDialog.value = true }) { // Show device dialog
                        Icon(Icons.Default.Home, contentDescription = "Show Devices", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                        if (bluetoothAdapter == null) {
                            Toast.makeText(context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
                        } else if (!bluetoothAdapter.isEnabled) {
                            Toast.makeText(context, "Bluetooth is off. Please turn it on.", Toast.LENGTH_SHORT).show()
                        } else {
                            val integrator = IntentIntegrator(context as android.app.Activity)
                            integrator.setOrientationLocked(false)
                            integrator.setPrompt("Scan a QR Code")
                            scanLauncher.launch(integrator.createScanIntent())
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "Scan QR Code"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLightTheme) androidx.compose.ui.graphics.Color(0xFFE0E0E0) else androidx.compose.ui.graphics.Color.DarkGray
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isLightTheme) Color.White else androidx.compose.ui.graphics.Color(0xFF303030))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Spinner for time interval - Full width
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth() // Full width for the dropdown menu
            ) {
                TextField(
                    value = selectedInterval,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Time Interval") },
                    trailingIcon = {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth() // Ensures TextField takes full width
                        .menuAnchor(),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = if (isLightTheme) Color.White else androidx.compose.ui.graphics.Color(0xFF424242),
                        focusedLabelColor = if (isLightTheme) Color.Black else Color.White,
                        unfocusedLabelColor = if (isLightTheme) Color.Black else Color.White
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth() // Ensures the dropdown menu is full width
                        .heightIn(max = 200.dp)
                ) {
                    timeIntervals.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval) },
                            onClick = {
                                selectedInterval = interval
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(availableApps) { app ->
                    AppListItem(
                        app = app,
                        isSelected = selectedApps.contains(app),
                        onClick = {
                            if (selectedApps.contains(app)) {
                                selectedApps.remove(app)
                            } else {
                                selectedApps.add(app)
                            }
                        }
                    )
                }
            }

            // PIN Code Input
            TextField(
                value = pinCode,
                onValueChange = { pinCode = it },
                label = { Text("Enter PIN Code") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = if (isLightTheme) Color.White else androidx.compose.ui.graphics.Color(0xFF424242),
                    focusedLabelColor = if (isLightTheme) Color.Black else Color.White,
                    unfocusedLabelColor = if (isLightTheme) Color.Black else Color.White,
                    focusedTextColor = if (isLightTheme) Color.Black else Color.White,
                    unfocusedTextColor = if (isLightTheme) Color.Black else Color.White,
                )
            )

            // Button at the bottom
            Button(
                onClick = {
                    if (pinCode.isNotEmpty() && selectedApps.isNotEmpty() && selectedInterval != "Select Interval") {
                        val intervalInMinutes = parseInterval(selectedInterval)
                        sendSelectedAppsToFirebase(selectedApps, intervalInMinutes, pinCode, context,devicesName)
                    } else {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF3F51B5)), // Indigo color
                shape = RoundedCornerShape(0.dp) // No corners
            ) {
                Text(
                    text = "Send Rules",
                    color = Color.White
                )
            }

            if (showDevicesDialog.value) {
                PairedDevicesDialog(
                    onDismiss = { showDevicesDialog.value = false },
                    onDeviceClick = { device ->
                        devicesName = device.name ?: "random"
                        Toast.makeText(context, "Clicked on ${device.name}", Toast.LENGTH_SHORT).show()
                        showDevicesDialog.value = false
                    }
                )
            }
        }
    }
}



@SuppressLint("MissingPermission")
fun scanDevices(context: Context,uuid : String) {

    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    if (bluetoothAdapter == null) {
        Log.e("Bluetooth", "Bluetooth not supported")
        return
    }

    val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                connectToDevice(context, device)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e("Bluetooth", "Scan failed with error code: $errorCode")
        }
    }

    val scanner = bluetoothAdapter.bluetoothLeScanner
    val filter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString(uuid))) // Replace with your UUID
        .build()

    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    scanner.startScan(listOf(filter), settings, scanCallback)
}

@SuppressLint("MissingPermission")
private fun connectToDevice(context: Context, device: BluetoothDevice) {
    var isConnected = false
    var isBondingStarted = false

    val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && !isConnected) {
                // Ensure Toast is shown only once for the connection
                isConnected = true
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "Device connected successfully", Toast.LENGTH_SHORT).show()
                }

                if (device.bondState == BluetoothDevice.BOND_BONDED) {
                    context.runOnUiThread {
                        Toast.makeText(context, "Device is already bonded", Toast.LENGTH_SHORT).show()
                    }
                } else if (!isBondingStarted) {
                    // Initiate bonding only if not already started
                    isBondingStarted = true
                    val bondSuccess = device.createBond()
                    if (bondSuccess) {
                        context.runOnUiThread {
                            Toast.makeText(context, "Bonding initiated successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        context.runOnUiThread {
                            Toast.makeText(context, "Failed to initiate bonding", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Handle discovered services if needed
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Handle the characteristic read response
            }
        }
    }
    device.connectGatt(context, false, gattCallback)
}




// Function to delete all apps from Firebase
fun deleteAllAppsFromFirebase(context: Context) {
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("Apps")
    firebaseDatabase.removeValue().addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Toast.makeText(context, "All apps deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to delete apps", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppListItem(app: InstalledApp, isSelected: Boolean, onClick: () -> Unit) {
    val iconPainter = rememberDrawablePainter(app.appIcon)

    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, SolidColor(Color.Blue), RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = iconPainter,
                contentDescription = app.appName,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                color = if (isSystemInDarkTheme()) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        val bitmap = drawable?.toBitmap(100, 100) ?: Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        BitmapPainter(bitmap.asImageBitmap())
    }
}

// Function to get installed apps
fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val apps = mutableListOf<InstalledApp>()
    val installedPackages = packageManager.getInstalledPackages(0)
    for (packageInfo in installedPackages) {
        if (packageInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) {
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val appPackage = packageInfo.packageName
            val appIcon = packageInfo.applicationInfo.loadIcon(packageManager)
            apps.add(InstalledApp(appName, appPackage, appIcon))
        }
    }
    return apps
}

// Data class for installed apps
data class InstalledApp(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable
)

@SuppressLint("NewApi")
fun drawableToByteArray(drawable: Drawable): ByteArray {
    val bitmap = when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is AdaptiveIconDrawable -> {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        else -> throw IllegalArgumentException("Unsupported drawable type")
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

@SuppressLint("MissingPermission")
@Composable
fun PairedDevicesDialog(
    onDismiss: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val pairedDevices = remember { mutableStateOf(bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Paired Devices",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (pairedDevices.value.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No paired devices found", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn {
                        items(pairedDevices.value) { device ->
                            DeviceListItem(
                                device = device,
                                onClick = { onDeviceClick(device) }
                            )
                        }
                    }
                }
            }
        }
    }


}

@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

fun sendSelectedAppsToFirebase(selectedApps: List<InstalledApp>, selectedInterval: Int,
                               pinCode: String, context: Context,deviceName : String) {
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("Apps")
        .child(deviceName.trim().toLowerCase(Locale.ROOT))

    firebaseDatabase.child("type").setValue("new data")
        .addOnSuccessListener {
            selectedApps.forEach { app ->
                val iconByteArray = app.appIcon?.let { drawableToByteArray(it) }

                val appData = mapOf(
                    "package_name" to app.packageName,
                    "name" to app.appName,
                    "interval" to selectedInterval.toString(),
                    "pin_code" to pinCode,
                    "icon" to iconByteArray?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
                )

                firebaseDatabase.child(app.appName.lowercase(Locale.ROOT)).setValue(appData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "uploaded successfully", LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error uploading ${app.appName}: ${e.message}", LENGTH_LONG).show()
                    }


            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error", LENGTH_LONG).show()
        }


}