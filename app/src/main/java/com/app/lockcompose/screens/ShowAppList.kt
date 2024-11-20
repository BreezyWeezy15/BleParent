

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import java.io.ByteArrayOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAppList() {
    val context = LocalContext.current

    val isLightTheme = !isSystemInDarkTheme()
    val allApps = remember { getInstalledApps(context) }
    val availableApps by remember { mutableStateOf(allApps.toMutableList()) }
    val selectedApps = remember { mutableStateListOf<InstalledApp>() }

    var expanded by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf("Select Interval") }
    val timeIntervals = listOf("1 min", "15 min", "30 min", "45 min", "60 min", "75 min", "90 min", "120 min")

    var pinCode by remember { mutableStateOf("") }

    // Bluetooth variables
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var pairedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

    // QR Code scan result
    var qrData by remember { mutableStateOf("") }

    // Function to parse interval (convert to minutes)
    fun parseInterval(interval: String): Int {
        return interval.replace(" min", "").toIntOrNull() ?: 0
    }

    // Function to delete all apps from Firebase
    fun deleteAllAppsFromFirebase() {
        val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("Apps")
        firebaseDatabase.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "All apps deleted", LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete apps", LENGTH_SHORT).show()
            }
        }
    }

    // Function to scan QR code
    fun startQRScanner() {
        val qrScanner = IntentIntegrator(context as Activity)
        qrScanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        qrScanner.setPrompt("Scan QR Code")
        qrScanner.initiateScan()
    }

    // Function to handle Bluetooth pairing (based on QR code)
    @SuppressLint("MissingPermission")
    fun startBluetoothDiscovery(qrData: String) {
        val deviceAddress = qrData // Assuming QR data contains device MAC address
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        pairedDevice = device
        bluetoothAdapter?.cancelDiscovery()
        bluetoothAdapter?.startDiscovery()

        // Connect to the Bluetooth device (if paired)
        if (pairedDevice != null) {
            Toast.makeText(context, "Attempting to connect to ${pairedDevice?.name}", LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Device not found", LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("》Rules list") },
                actions = {
                    IconButton(onClick = {
                        if (availableApps.isEmpty()) {
                          //  makeText(context, "No apps to delete", LENGTH_SHORT).show()
                        } else {
                            deleteAllAppsFromFirebase()
                            availableApps.clear()  // Clear the list locally
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Apps"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLightTheme) Color(0xFFE0E0E0) else Color.DarkGray
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isLightTheme) Color.White else Color(0xFF303030))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // QR Scanner Button
            Button(
                onClick = {
                    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                        // Bluetooth is not enabled, prompt user to enable it
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        (context as Activity).startActivityForResult(enableBtIntent, 1)
                    } else {
                        startQRScanner()
                    } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)) // Indigo color
            ) {
                Text("Scan QR Code", color = Color.White)
            }

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
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth() // Ensures TextField takes full width
                        .menuAnchor(),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = if (isLightTheme) Color.White else Color(0xFF424242),
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

            Spacer(modifier = Modifier.height(16.dp)) // Space between spinner and list

            // LazyColumn for the list of apps
            LazyColumn(
                modifier = Modifier.weight(1f) // Allow the list to take remaining space
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
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = if (isLightTheme) Color.White else Color(0xFF424242),
                    focusedLabelColor = if (isLightTheme) Color.Black else Color.White,
                    unfocusedLabelColor = if (isLightTheme) Color.Black else Color.White,
                    focusedTextColor = if (isLightTheme) Color.Black else Color.White,
                    unfocusedTextColor = if (isLightTheme) Color.Black else Color.White,
                )
            )

            // Send Button at the bottom
            Button(
                onClick = {
                    if (pinCode.isNotEmpty() && selectedApps.isNotEmpty() && selectedInterval != "Select Interval") {
                        val intervalInMinutes = parseInterval(selectedInterval)
                        sendSelectedAppsToFirebase(selectedApps, intervalInMinutes, pinCode, context)
                    } else {
                        Toast.makeText(context, "Please fill all required fields", LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)), // Indigo color
                shape = RoundedCornerShape(0.dp) // No corners
            ) {
                Text(
                    text = "Send Rules",
                    color = Color.White
                )
            }
        }
    }
}

fun sendSelectedAppsToFirebase(
    selectedApps: List<InstalledApp>,
    intervalInMinutes: Int,
    pinCode: String,
    context: Context
) {
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("UserApps")
    val userAppData = selectedApps.map { app ->
        mapOf(
            "packageName" to app.packageName,
            "appName" to app.appName,
            "intervalInMinutes" to intervalInMinutes,
            "pinCode" to pinCode
        )
    }

    firebaseDatabase.push().setValue(userAppData).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Toast.makeText(context, "Apps sent successfully", LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to send apps", LENGTH_SHORT).show()
        }
    }
}


fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val installedPackages = packageManager.getInstalledApplications(0)
    return installedPackages.map {
        InstalledApp(
            appName = it.loadLabel(packageManager).toString(),
            packageName = it.packageName,
            icon = it.loadIcon(packageManager)
        )
    }
}

data class InstalledApp(val appName: String, val packageName: String, val icon: Drawable)

@Composable
fun AppListItem(app: InstalledApp, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .background(if (isSelected) Color(0xFFE1F5FE) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(app.icon),
            contentDescription = "App Icon",
            modifier = Modifier
                .size(40.dp)
                .padding(end = 16.dp)
        )

        Text(app.appName, style = MaterialTheme.typography.bodyMedium)
    }
}

fun rememberDrawablePainter(drawable: Drawable): Painter {
    val bitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }
    return BitmapPainter(bitmap.asImageBitmap())
}
