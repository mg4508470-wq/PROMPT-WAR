package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.BloodGroup
import com.example.data.model.BloodRequest
import com.example.data.model.Hospital
import com.example.data.model.UserProfile
import com.example.data.repository.LifeDropRepository
import com.example.data.util.NotificationHelper
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed class MapEntity {
    data class RequestMarker(val request: BloodRequest) : MapEntity()
    data class HospitalMarker(val hospital: Hospital) : MapEntity()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveMapScreen(
    userProfile: UserProfile,
    verifiedRequests: List<BloodRequest>,
    hospitals: List<Hospital>,
    repository: LifeDropRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListView by remember { mutableStateOf(false) }
    var selectedFilterGroup by remember { mutableStateOf("All") }
    var selectedTypeFilter by remember { mutableStateOf("All") } // "All", "Requests", "Hospitals", "Blood Banks"
    var selectedUrgency by remember { mutableStateOf("All") } // "All", "Critical", "Urgent"

    // Seed sample hospitals if empty on first launch
    LaunchedEffect(Unit) {
        repository.seedSampleHospitalsIfEmpty()
    }

    // Location state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLat by remember { mutableFloatStateOf(13.0827f) }
    var userLng by remember { mutableFloatStateOf(80.2707f) }
    var locationStatus by remember { mutableStateOf("Default Area: Springfield") }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun refreshLocation() {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        userLat = loc.latitude.toFloat()
                        userLng = loc.longitude.toFloat()
                        locationStatus = "Live GPS: ${String.format("%.4f", userLat)}, ${String.format("%.4f", userLng)}"
                    }
                }
            } catch (_: SecurityException) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            refreshLocation()
            Toast.makeText(context, "Location permission granted! Showing nearby requests.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            refreshLocation()
        }
    }

    // Selected marker for bottom sheet detail
    var selectedEntity by remember { mutableStateOf<MapEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Filtered requests and hospitals
    val displayedRequests = remember(verifiedRequests, selectedFilterGroup, selectedTypeFilter, selectedUrgency) {
        if (selectedTypeFilter == "Hospitals" || selectedTypeFilter == "Blood Banks") {
            emptyList()
        } else {
            verifiedRequests.filter { req ->
                val groupMatch = selectedFilterGroup == "All" || req.bloodGroup == selectedFilterGroup ||
                        BloodGroup.isCompatible(userProfile.bloodGroup, req.bloodGroup)
                val urgencyMatch = selectedUrgency == "All" || req.urgency.equals(selectedUrgency, ignoreCase = true)
                groupMatch && urgencyMatch
            }
        }
    }

    val displayedHospitals = remember(hospitals, selectedTypeFilter) {
        if (selectedTypeFilter == "Requests") {
            emptyList()
        } else if (selectedTypeFilter == "Blood Banks") {
            hospitals.filter { it.type == "blood_bank" }
        } else if (selectedTypeFilter == "Hospitals") {
            hospitals.filter { it.type == "hospital" }
        } else {
            hospitals
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live Blood Map", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = locationStatus,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        Icon(
                            if (isListView) Icons.Default.Map else Icons.Default.FormatListBulleted,
                            contentDescription = if (isListView) "Map View" else "List View"
                        )
                    }
                    IconButton(onClick = {
                        refreshLocation()
                        Toast.makeText(context, "Map refreshed with latest hospital broadcasts", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("live_map_screen")
        ) {
            // Filter Strip
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Requests", "Hospitals", "Blood Banks").forEach { type ->
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = { selectedTypeFilter = type },
                                label = { Text(type, fontSize = 11.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Compatible", "Critical", "O+", "A+", "B+", "AB-").forEach { filter ->
                            FilterChip(
                                selected = selectedFilterGroup == filter,
                                onClick = { selectedFilterGroup = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                modifier = Modifier.height(30.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (!hasLocationPermission) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable GPS to find nearest emergency hospitals",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Body: Interactive Live Map or List View
            if (isListView) {
                // List View alternative
                RequestsAndHospitalsListView(
                    requests = displayedRequests,
                    hospitals = displayedHospitals,
                    userProfile = userProfile,
                    onSelectRequest = { selectedEntity = MapEntity.RequestMarker(it) },
                    onSelectHospital = { selectedEntity = MapEntity.HospitalMarker(it) }
                )
            } else {
                // Interactive Vector / Radar Live Map
                InteractiveLiveMapView(
                    centerLat = userLat,
                    centerLng = userLng,
                    requests = displayedRequests,
                    hospitals = displayedHospitals,
                    onMarkerClick = { entity -> selectedEntity = entity },
                    onGpsClick = {
                        if (!hasLocationPermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            refreshLocation()
                            Toast.makeText(context, "Centered to your GPS location", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Modal Bottom Sheet for marker details
    if (selectedEntity != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedEntity = null },
            sheetState = sheetState
        ) {
            when (val entity = selectedEntity!!) {
                is MapEntity.RequestMarker -> {
                    val req = entity.request
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = req.bloodGroup,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${req.units} Unit(s) Needed",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Status: ${req.status.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (req.urgency == "Critical") Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = req.urgency.uppercase(),
                                    color = if (req.urgency == "Critical") Color(0xFFD32F2F) else Color(0xFFF57C00),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = req.hospitalName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Address: ${req.hospitalAddress.ifBlank { req.city }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Required by: ${req.requiredDate} (${req.requiredTime})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons: Open in Google Maps, I'm Available, Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val lat = if (req.latitude != 0.0) req.latitude else 13.0827
                                    val lng = if (req.longitude != 0.0) req.longitude else 80.2707
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=$lat,$lng(${Uri.encode(req.hospitalName)})")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                                        context.startActivity(fallbackIntent)
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Directions", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    NotificationHelper.showBloodAlertNotification(
                                        context = context,
                                        notificationId = req.id.hashCode(),
                                        bloodGroup = req.bloodGroup,
                                        hospitalName = req.hospitalName,
                                        urgency = req.urgency,
                                        units = req.units,
                                        city = req.city
                                    )
                                    Toast.makeText(context, "Confirmed availability for ${req.hospitalName}! Requester notified.", Toast.LENGTH_LONG).show()
                                    selectedEntity = null
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("I'm Available", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${req.requesterPhone}")
                                    }
                                    try { context.startActivity(callIntent) } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                is MapEntity.HospitalMarker -> {
                    val hosp = entity.hospital
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (hosp.type == "blood_bank") MaterialTheme.colorScheme.primary else Color(0xFF1976D2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (hosp.type == "blood_bank") Icons.Default.Bloodtype else Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = hosp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (hosp.type == "blood_bank") "Official Blood Bank" else "Participating Hospital",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(text = "Address: ${hosp.address}, ${hosp.city}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Available Donation Units/Beds: ${hosp.availableBeds}", style = MaterialTheme.typography.bodySmall)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=${hosp.latitude},${hosp.longitude}(${Uri.encode(hosp.name)})")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (_: Exception) {
                                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${hosp.latitude},${hosp.longitude}"))
                                        context.startActivity(fallback)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate to Hospital")
                            }

                            if (hosp.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${hosp.phone}")
                                        }
                                        try { context.startActivity(callIntent) } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveLiveMapView(
    centerLat: Float,
    centerLng: Float,
    requests: List<BloodRequest>,
    hospitals: List<Hospital>,
    onMarkerClick: (MapEntity) -> Unit,
    onGpsClick: () -> Unit
) {
    // Pan & Zoom gestures
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Pulse animation for critical markers
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Night map grid theme
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        // Map Grid & Radar Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2 + offsetX
            val centerY = h / 2 + offsetY

            // Map grid lines
            val gridSize = 60f * scale
            var x = centerX % gridSize
            while (x < w) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += gridSize
            }
            var y = centerY % gridSize
            while (y < h) {
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }

            // Radius radar rings
            listOf(120f, 240f, 360f).forEach { radius ->
                drawCircle(
                    color = Color(0xFF334155).copy(alpha = 0.5f),
                    radius = radius * scale,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.5f)
                )
            }

            // Radar pulse wave
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = (1f - radarPulse) * 0.35f),
                radius = radarPulse * 380f * scale,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.5f)
            )

            // Center User Location Marker
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.25f),
                radius = 24f * scale,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF00E676),
                radius = 8f * scale,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 3f * scale,
                center = Offset(centerX, centerY)
            )
        }

        // Floating GPS center button
        IconButton(
            onClick = {
                offsetX = 0f
                offsetY = 0f
                scale = 1f
                onGpsClick()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = MaterialTheme.colorScheme.primary)
        }

        // Map Legend Pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = Color(0xFFD32F2F), label = "Emergency")
                LegendIndicator(color = Color(0xFF1976D2), label = "Hospital")
                LegendIndicator(color = Color(0xFF00897B), label = "Blood Bank")
            }
        }

        // Overlay Interactive Request Markers
        Box(modifier = Modifier.fillMaxSize()) {
            val zoom = scale
            val refLat = centerLat
            val refLng = centerLng

            // 1. Render Requests
            requests.forEachIndexed { index, req ->
                val rLat = if (req.latitude != 0.0) req.latitude.toFloat() else (centerLat + (index * 0.015f - 0.03f))
                val rLng = if (req.longitude != 0.0) req.longitude.toFloat() else (centerLng + ((index % 2) * 0.02f - 0.01f))

                // Map geo-diff to screen pixels
                val dx = (rLng - refLng) * 8000f * zoom + offsetX
                val dy = -(rLat - refLat) * 8000f * zoom + offsetY

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                        .clickable { onMarkerClick(MapEntity.RequestMarker(req)) }
                ) {
                    MapRequestPin(
                        bloodGroup = req.bloodGroup,
                        isCritical = req.urgency == "Critical"
                    )
                }
            }

            // 2. Render Hospitals & Blood Banks
            hospitals.forEachIndexed { index, hosp ->
                val hLat = hosp.latitude.toFloat()
                val hLng = hosp.longitude.toFloat()

                val dx = (hLng - refLng) * 8000f * zoom + offsetX
                val dy = -(hLat - refLat) * 8000f * zoom + offsetY

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                        .clickable { onMarkerClick(MapEntity.HospitalMarker(hosp)) }
                ) {
                    MapHospitalPin(isBloodBank = hosp.type == "blood_bank")
                }
            }
        }
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun MapRequestPin(bloodGroup: String, isCritical: Boolean) {
    val bgColor = if (isCritical) Color(0xFFD32F2F) else Color(0xFFE65100)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bloodGroup,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }

        // Pointer triangle
        Canvas(modifier = Modifier.size(8.dp, 6.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(path, bgColor)
        }
    }
}

@Composable
fun MapHospitalPin(isBloodBank: Boolean) {
    val color = if (isBloodBank) Color(0xFF00897B) else Color(0xFF1976D2)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isBloodBank) Icons.Default.Bloodtype else Icons.Default.LocalHospital,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun RequestsAndHospitalsListView(
    requests: List<BloodRequest>,
    hospitals: List<Hospital>,
    userProfile: UserProfile,
    onSelectRequest: (BloodRequest) -> Unit,
    onSelectHospital: (Hospital) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Active Urgent Blood Requests (${requests.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (requests.isEmpty()) {
            item {
                Text(
                    text = "No matching requests in this filter range.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(requests, key = { it.id }) { req ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRequest(req) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = req.bloodGroup, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${req.units} Unit(s) at ${req.hospitalName}", fontWeight = FontWeight.Bold)
                            Text(
                                text = "City: ${req.city} • Urgency: ${req.urgency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.Directions, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Verified Nearby Hospitals & Blood Banks (${hospitals.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(hospitals, key = { it.id }) { hosp ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectHospital(hosp) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (hosp.type == "blood_bank") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF1976D2).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (hosp.type == "blood_bank") Icons.Default.Bloodtype else Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = if (hosp.type == "blood_bank") MaterialTheme.colorScheme.primary else Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = hosp.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${hosp.address}, ${hosp.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Directions, contentDescription = "Navigate", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
