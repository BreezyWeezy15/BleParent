package com.app.lockcompose.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

@SuppressLint("MissingPermission")
@Composable
fun DevicesScreen() {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    // Ensure Bluetooth is supported and enabled
    if (bluetoothAdapter == null) {
        Text(
            text = "Bluetooth is not supported on this device",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Red,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val isBluetoothEnabled = bluetoothAdapter.isEnabled
    if (!isBluetoothEnabled) {
        Text(
            text = "Please turn on Bluetooth",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Red,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    // Remember the list of paired devices
    var pairedDevices = remember { mutableStateOf(bluetoothAdapter.bondedDevices.toList()) }

    // Manually refresh paired devices list if necessary
    LaunchedEffect(Unit) {
        pairedDevices.value = bluetoothAdapter.bondedDevices.toList()
    }

    // Step 2: Display paired devices in a scrollable list
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Paired Devices",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Step 3: If there are no paired devices
        if (pairedDevices.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No paired devices found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn {
                items(pairedDevices.value) { device ->
                    DeviceListItem(device = device)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(device: BluetoothDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
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

            IconButton(onClick = { /* Handle device click (e.g., connect) */ }) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "Connect")
            }
        }
    }
}
