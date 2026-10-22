package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BloodGroup
import com.example.data.model.BloodRequest
import com.example.data.repository.LifeDropRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BloodRequestFormScreen(
    currentUserId: String,
    currentUserPhone: String,
    currentUserName: String,
    userCity: String,
    repository: LifeDropRepository,
    onNavigateBack: () -> Unit,
    onRequestSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var patientRef by remember { mutableStateOf("") }
    var selectedBloodGroup by remember { mutableStateOf("O+") }
    var units by remember { mutableIntStateOf(1) }
    var hospitalName by remember { mutableStateOf("") }
    var hospitalAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf(userCity.ifBlank { "Springfield" }) }
    var district by remember { mutableStateOf("Downtown") }
    var requesterPhone by remember { mutableStateOf(currentUserPhone) }
    var urgency by remember { mutableStateOf("Urgent") } // Normal, Urgent, Critical
    var requiredDate by remember { mutableStateOf("Today (Immediate)") }
    var requiredTime by remember { mutableStateOf("Within 4 hours") }
    var notes by remember { mutableStateOf("") }
    var consentChecked by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Blood Donation", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Privacy Notice
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Patient Privacy: Use reference ID or first name only. Sensitive health records are never published.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Blood Group Needed
            Text(
                text = "Required Blood Group *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val bloodGroups = BloodGroup.entries.map { it.display }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                bloodGroups.forEach { bg ->
                    val isSelected = bg == selectedBloodGroup
                    Box(
                        modifier = Modifier
                            .size(68.dp, 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedBloodGroup = bg }
                            .testTag("request_blood_group_$bg"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bg,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Units Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Number of Units (Bags) *",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Standard pint/unit per donor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { if (units > 1) units-- },
                        enabled = units > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = "$units",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = { if (units < 10) units++ },
                        enabled = units < 10
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            // Urgency Selector
            Text(
                text = "Emergency Priority Level *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Normal", "Urgent", "Critical").forEach { level ->
                    val isSelected = urgency == level
                    val levelColor = when (level) {
                        "Critical" -> Color(0xFFD32F2F)
                        "Urgent" -> Color(0xFFF57C00)
                        else -> Color(0xFF388E3C)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) levelColor else levelColor.copy(alpha = 0.12f))
                            .clickable { urgency = level }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = level,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else levelColor
                        )
                    }
                }
            }

            // Hospital Details
            OutlinedTextField(
                value = hospitalName,
                onValueChange = { hospitalName = it },
                label = { Text("Hospital Name *") },
                leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hospital_name_input"),
                singleLine = true
            )

            OutlinedTextField(
                value = hospitalAddress,
                onValueChange = { hospitalAddress = it },
                label = { Text("Hospital Address / Floor / Room") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("District") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Patient Identifier & Contact
            OutlinedTextField(
                value = patientRef,
                onValueChange = { patientRef = it },
                label = { Text("Patient Ref ID (e.g. ICU-Bed-4, or Name) *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = requesterPhone,
                onValueChange = { requesterPhone = it },
                label = { Text("Requester Contact Phone *") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = requiredDate,
                    onValueChange = { requiredDate = it },
                    label = { Text("Required Date") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = requiredTime,
                    onValueChange = { requiredTime = it },
                    label = { Text("Required Time") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Clinical Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Medical Safety Consent
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = consentChecked,
                    onCheckedChange = { consentChecked = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I confirm this is a genuine medical requirement. I understand requests undergo admin verification before broadcast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (hospitalName.isBlank()) {
                        errorMessage = "Please enter the hospital name."
                        return@Button
                    }
                    if (city.isBlank()) {
                        errorMessage = "Please enter the city."
                        return@Button
                    }
                    if (requesterPhone.isBlank()) {
                        errorMessage = "Please enter contact phone number."
                        return@Button
                    }
                    if (!consentChecked) {
                        errorMessage = "Please confirm the genuine medical requirement checkbox."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    scope.launch {
                        val newRequest = BloodRequest(
                            requesterId = currentUserId,
                            requesterName = currentUserName.ifBlank { "Patient Attendant" },
                            requesterPhone = requesterPhone.trim(),
                            patientRef = patientRef.ifBlank { "Emergency Case" }.trim(),
                            bloodGroup = selectedBloodGroup,
                            units = units,
                            hospitalName = hospitalName.trim(),
                            hospitalAddress = hospitalAddress.trim(),
                            city = city.trim(),
                            district = district.trim(),
                            urgency = urgency,
                            requiredDate = requiredDate.trim(),
                            requiredTime = requiredTime.trim(),
                            status = "pending",
                            notes = notes.trim()
                        )

                        val result = repository.createBloodRequest(newRequest)
                        isSubmitting = false
                        if (result.isSuccess) {
                            Toast.makeText(
                                context,
                                "Blood request submitted! It will be verified by the admin team shortly.",
                                Toast.LENGTH_LONG
                            ).show()
                            onRequestSubmitted()
                        } else {
                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit request."
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_blood_request_button"),
                shape = RoundedCornerShape(27.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Submitting Request...")
                } else {
                    Text(
                        text = "Submit for Verification",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
