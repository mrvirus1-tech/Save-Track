package com.example.ui.screens.rider

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Parcel
import com.example.data.model.ParcelStatus
import com.example.data.model.Shift
import com.example.data.model.UiState
import com.example.data.model.User
import com.example.ui.components.CameraXCaptureView
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.ParcelStatusBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ShiftBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.viewmodel.RiderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboard(
    rider: User,
    riderViewModel: RiderViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activeShift by riderViewModel.activeShiftState.collectAsStateWithLifecycle()
    val parcelsState by riderViewModel.assignedParcelsState.collectAsStateWithLifecycle()
    val isStartingShift by riderViewModel.isStartingShift.collectAsStateWithLifecycle()
    val statusMessage by riderViewModel.statusMessage.collectAsStateWithLifecycle()

    var showCameraView by remember { mutableStateOf(false) }
    var failingParcel by remember { mutableStateOf<Parcel?>(null) }
    var selectedTrackingParcel by remember { mutableStateOf<Parcel?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            riderViewModel.clearStatusMessage()
        }
    }

    // Permission Launcher for Camera and Location
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (cameraGranted) {
            showCameraView = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(rider.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text(
                            text = if (activeShift != null) "Shift Active • GPS Broadcasting" else "Off Duty",
                            fontSize = 11.sp,
                            color = if (activeShift != null) CyanAccent else Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    ShiftBadge(status = if (activeShift != null) "ACTIVE" else "COMPLETED")
                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("rider_sign_out_button")) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = Color(0xFFEF4444))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Shift Attendance & Status Controller Card
                ShiftStatusCard(
                    activeShift = activeShift,
                    isStarting = isStartingShift,
                    onStartShiftClick = {
                        permissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onEndShiftClick = {
                        activeShift?.let { riderViewModel.endShift(context, it.id) }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Assigned Deliveries Header
                Text(
                    text = "My Assigned Parcels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (val state = parcelsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyanAccent)
                        }
                    }
                    is UiState.Error -> {
                        EmptyStateView(
                            icon = Icons.Default.Inbox,
                            title = "Error Loading Deliveries",
                            description = state.message
                        )
                    }
                    is UiState.Success -> {
                        val parcels = state.data
                        if (parcels.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Inbox,
                                title = "No Parcels Assigned",
                                description = "You have no active parcel assignments. When an office manager assigns deliveries to your account, they will automatically appear here."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(parcels) { parcel ->
                                    RiderParcelCard(
                                        parcel = parcel,
                                        isShiftActive = activeShift != null,
                                        onStatusChange = { newStatus ->
                                            riderViewModel.updateParcelStatus(parcel.id, newStatus)
                                        },
                                        onFailClick = { failingParcel = parcel },
                                        onTrackClick = { selectedTrackingParcel = parcel },
                                        onCallRecipient = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${parcel.recipientPhone}"))
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // In-app CameraX front-camera viewfinder overlay for shift selfie
            if (showCameraView) {
                CameraXCaptureView(
                    onDismiss = { showCameraView = false },
                    onCaptured = { base64Selfie ->
                        showCameraView = false
                        riderViewModel.startShiftWithSelfie(context, base64Selfie) {}
                    }
                )
            }
        }
    }

    failingParcel?.let { parcel ->
        ReportFailedDeliveryDialog(
            parcel = parcel,
            onDismiss = { failingParcel = null },
            onSubmit = { reason ->
                riderViewModel.updateParcelStatus(parcel.id, ParcelStatus.FAILED, reason)
                failingParcel = null
            }
        )
    }

    selectedTrackingParcel?.let { parcel ->
        RiderRouteMapDialog(
            parcel = parcel,
            onDismiss = { selectedTrackingParcel = null }
        )
    }
}

@Composable
private fun ShiftStatusCard(
    activeShift: Shift?,
    isStarting: Boolean,
    onStartShiftClick: () -> Unit,
    onEndShiftClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (activeShift != null) Navy800 else Color(0xFF1E293B)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (activeShift != null) CyanAccent else Navy700
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (activeShift != null) "Active Work Shift" else "Shift Inactive",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (activeShift != null) "Foreground GPS service active" else "Attendance selfie required before starting",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                if (activeShift == null) {
                    Button(
                        onClick = onStartShiftClick,
                        enabled = !isStarting,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("start_shift_button")
                    ) {
                        if (isStarting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Navy900, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Shift", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onEndShiftClick,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("end_shift_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Shift")
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderParcelCard(
    parcel: Parcel,
    isShiftActive: Boolean,
    onStatusChange: (ParcelStatus) -> Unit,
    onFailClick: () -> Unit,
    onTrackClick: () -> Unit,
    onCallRecipient: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = Modifier.fillMaxWidth().testTag("rider_parcel_${parcel.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${parcel.trackingNumber}",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 15.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityBadge(priority = parcel.priority)
                    ParcelStatusBadge(status = parcel.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(parcel.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Text("Pickup: ${parcel.senderAddress}", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text("Deliver to: ${parcel.deliveryAddress}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${parcel.recipientName} (${parcel.recipientPhone})",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )

                if (parcel.recipientPhone.isNotBlank()) {
                    IconButton(onClick = onCallRecipient, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = CyanAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (parcel.status) {
                    ParcelStatus.ASSIGNED.name -> {
                        Button(
                            onClick = { onStatusChange(ParcelStatus.PICKED_UP) },
                            enabled = isShiftActive,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("action_picked_up")
                        ) {
                            Text("Mark Picked Up", fontWeight = FontWeight.Bold)
                        }
                    }

                    ParcelStatus.PICKED_UP.name -> {
                        Button(
                            onClick = { onStatusChange(ParcelStatus.IN_TRANSIT) },
                            enabled = isShiftActive,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("action_in_transit")
                        ) {
                            Text("Start Delivery (In Transit)", fontWeight = FontWeight.Bold)
                        }
                    }

                    ParcelStatus.IN_TRANSIT.name -> {
                        Button(
                            onClick = { onStatusChange(ParcelStatus.ARRIVED) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("action_arrived")
                        ) {
                            Text("Arrived at Target", fontWeight = FontWeight.Bold)
                        }
                    }

                    ParcelStatus.ARRIVED.name -> {
                        Button(
                            onClick = { onStatusChange(ParcelStatus.DELIVERED) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("action_delivered")
                        ) {
                            Text("Deliver Package", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onFailClick,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("action_report_failed")
                        ) {
                            Text("Report Failed")
                        }
                    }

                    ParcelStatus.DELIVERED.name -> {
                        Text("Delivery Complete ✓", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    ParcelStatus.FAILED.name -> {
                        Text("Failed: ${parcel.failureReason}", color = StatusRed, fontSize = 12.sp)
                    }
                }

                // Map & External Navigation button
                if (parcel.destLat != 0.0 && parcel.destLng != 0.0 && parcel.status != ParcelStatus.DELIVERED.name) {
                    IconButton(
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=${parcel.destLat},${parcel.destLng}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                onTrackClick()
                            }
                        },
                        modifier = Modifier
                            .background(Navy700, RoundedCornerShape(8.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = "Navigate", tint = CyanAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportFailedDeliveryDialog(
    parcel: Parcel,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("Recipient not available at address") }
    val reasons = listOf(
        "Recipient not available at address",
        "Wrong address provided",
        "Recipient refused delivery",
        "Damaged parcel packaging",
        "Unable to access building/gate"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Failed Delivery", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select operational exception reason:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                reasons.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (reason == item) Navy700 else Navy800, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = reason == item,
                            onClick = { reason = item }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed, contentColor = Color.White)
            ) {
                Text("Submit Exception")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = Navy900
    )
}

@Composable
private fun RiderRouteMapDialog(
    parcel: Parcel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Route: ${parcel.title}", color = Color.White) },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                InteractiveMapView(
                    riderLat = parcel.pickupLat,
                    riderLng = parcel.pickupLng,
                    pickupLat = parcel.pickupLat,
                    pickupLng = parcel.pickupLng,
                    pickupAddress = parcel.senderAddress,
                    destLat = parcel.destLat,
                    destLng = parcel.destLng,
                    deliveryAddress = parcel.deliveryAddress,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900)) {
                Text("Close")
            }
        },
        containerColor = Navy900
    )
}
