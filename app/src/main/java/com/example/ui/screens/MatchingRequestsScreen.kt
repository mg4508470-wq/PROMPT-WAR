package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodGroup
import com.example.data.model.BloodRequest
import com.example.data.model.UserProfile
import com.example.data.repository.LifeDropRepository
import com.example.data.util.NotificationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingRequestsScreen(
    userProfile: UserProfile,
    verifiedRequests: List<BloodRequest>,
    repository: LifeDropRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Compatible") } // Compatible, All, Critical, Near Me
    var reportedRequest by remember { mutableStateOf<BloodRequest?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    // Track user's availability response for this session
    val respondedRequests = remember { mutableStateOf(mutableMapOf<String, String>()) }

    val filteredList = remember(verifiedRequests, userProfile, selectedFilter, searchQuery) {
        verifiedRequests.filter { req ->
            val matchesSearch = searchQuery.isBlank() ||
                    req.hospitalName.contains(searchQuery, ignoreCase = true) ||
                    req.city.contains(searchQuery, ignoreCase = true) ||
                    req.bloodGroup.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Compatible" -> BloodGroup.isCompatible(userProfile.bloodGroup, req.bloodGroup)
                "Critical" -> req.urgency == "Critical"
                "Near Me" -> req.city.equals(userProfile.city, ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matching Blood Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("matching_requests_screen")
        ) {
            // Search and filter bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by hospital, city, blood group...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Compatible", "Near Me", "Critical", "All").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            leadingIcon = if (filter == "Compatible") {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Requests Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try switching filters to 'All' or checking your radius settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredList, key = { it.id }) { req ->
                        val userResponse = respondedRequests.value[req.id]

                        MatchingRequestItem(
                            request = req,
                            donorGroup = userProfile.bloodGroup,
                            userResponse = userResponse,
                            onAvailable = {
                                respondedRequests.value = respondedRequests.value.toMutableMap().apply {
                                    put(req.id, "AVAILABLE")
                                }
                                NotificationHelper.showBloodAlertNotification(
                                    context = context,
                                    notificationId = req.id.hashCode(),
                                    bloodGroup = req.bloodGroup,
                                    hospitalName = req.hospitalName,
                                    urgency = req.urgency,
                                    units = req.units,
                                    city = req.city
                                )
                                Toast.makeText(
                                    context,
                                    "Response Sent: You confirmed availability for ${req.hospitalName}! Requester can now contact you.",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onNotAvailable = {
                                respondedRequests.value = respondedRequests.value.toMutableMap().apply {
                                    put(req.id, "NOT_AVAILABLE")
                                }
                                Toast.makeText(context, "Marked as not available.", Toast.LENGTH_SHORT).show()
                            },
                            onContact = {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${req.requesterPhone}")
                                }
                                try {
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Call: ${req.requesterPhone}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReport = {
                                reportedRequest = req
                                reportReason = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Report Dialog
    if (reportedRequest != null) {
        AlertDialog(
            onDismissRequest = { reportedRequest = null },
            title = { Text("Report Suspicious Request", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Help us maintain LifeDrop trust. Please describe why you suspect this request for ${reportedRequest?.hospitalName} is fraudulent or invalid:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("e.g. Inactive phone number, false hospital address, asking for fees") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isBlank()) {
                            Toast.makeText(context, "Please enter a reason", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingReport = true
                        scope.launch {
                            val res = repository.submitReport(
                                requestId = reportedRequest?.id ?: "",
                                reason = reportReason.trim()
                            )
                            isSubmittingReport = false
                            reportedRequest = null
                            if (res.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Report submitted. Our safety team will investigate immediately.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Failed to submit report", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportedRequest = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MatchingRequestItem(
    request: BloodRequest,
    donorGroup: String,
    userResponse: String?,
    onAvailable: () -> Unit,
    onNotAvailable: () -> Unit,
    onContact: () -> Unit,
    onReport: () -> Unit
) {
    val isCompatible = BloodGroup.isCompatible(donorGroup, request.bloodGroup)
    val urgencyColor = when (request.urgency) {
        "Critical" -> Color(0xFFD32F2F)
        "Urgent" -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Blood group, units, Urgency badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = request.bloodGroup,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${request.units} Unit(s) Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCompatible) "✓ Compatible with your $donorGroup blood" else "Alternative group match",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompatible) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(urgencyColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = request.urgency.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = urgencyColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location & Hospital
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${request.hospitalName}, ${request.hospitalAddress.ifBlank { request.city }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "City: ${request.city} • Required by: ${request.requiredDate} (${request.requiredTime})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (request.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Clinical note: ${request.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Response Banner if responded
            if (userResponse == "AVAILABLE") {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You confirmed availability! Thank you for being a lifesaver.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            } else if (userResponse == "NOT_AVAILABLE") {
                Text(
                    text = "You responded: Not Available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Action Buttons: I'm Available, Contact, Not Available, Report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAvailable,
                    enabled = userResponse != "AVAILABLE",
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (userResponse == "AVAILABLE") "Confirmed" else "I'm Available", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onContact,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 13.sp)
                }

                if (userResponse == null) {
                    OutlinedButton(
                        onClick = onNotAvailable,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Text("Decline", fontSize = 12.sp)
                    }
                }

                IconButton(
                    onClick = onReport,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = "Report",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
