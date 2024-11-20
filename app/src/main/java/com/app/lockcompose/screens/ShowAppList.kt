import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.widget.Toast
import android.widget.Toast.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.app.lockcompose.R
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
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

    fun parseInterval(interval: String): Int {
        return interval.replace(" min", "").toIntOrNull() ?: 0
    }

    // Function to delete all apps from Firebase
    fun deleteAllAppsFromFirebase() {
        val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("Apps")
        firebaseDatabase.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                makeText(context, "All apps deleted", LENGTH_SHORT).show()
            } else {
                makeText(context, "Failed to delete apps", LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("》Rules list") },
                actions = {
                    IconButton(onClick = {
                        if (availableApps.isEmpty()) {
                            makeText(context, "No apps to delete", LENGTH_SHORT).show()
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

            // Button at the bottom
            Button(
                onClick = {
                    if (pinCode.isNotEmpty() && selectedApps.isNotEmpty() && selectedInterval != "Select Interval") {
                        val intervalInMinutes = parseInterval(selectedInterval)
                        sendSelectedAppsToFirebase(selectedApps, intervalInMinutes, pinCode, context)
                    } else {
                        makeText(context, "Please fill all required fields", LENGTH_SHORT).show()
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

data class InstalledApp(
    val packageName: String,
    val name: String,
    val icon: Drawable?
)

fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val appsList = mutableListOf<InstalledApp>()
    val installedApps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN, null), 0)

    installedApps.forEach { resolveInfo ->
        val appName = resolveInfo.loadLabel(packageManager).toString()
        val appIcon = resolveInfo.loadIcon(packageManager)
        appsList.add(InstalledApp(resolveInfo.activityInfo.packageName, appName, appIcon))
    }

    return appsList
}

fun sendSelectedAppsToFirebase(selectedApps: List<InstalledApp>, intervalInMinutes: Int, pinCode: String, context: Context) {
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("SelectedApps")
    val appsData = selectedApps.map { app ->
        mapOf(
            "package" to app.packageName,
            "name" to app.name,
            "interval" to intervalInMinutes,
            "pinCode" to pinCode
        )
    }
    firebaseDatabase.push().setValue(appsData).addOnCompleteListener {
        if (it.isSuccessful) {
            makeText(context, "Data sent successfully", LENGTH_SHORT).show()
        } else {
            makeText(context, "Failed to send data", LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppListItem(
    app: InstalledApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE0E0E0) else Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val iconPainter: Painter = if (app.icon != null) {
                BitmapPainter((app.icon as BitmapDrawable).bitmap.asImageBitmap())
            } else {
                painterResource(id = R.drawable.ic_launcher_foreground)
            }
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.Black else Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
