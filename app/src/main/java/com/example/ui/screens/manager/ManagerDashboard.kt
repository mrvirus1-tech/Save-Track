package com.example.ui.screens.manager

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Parcel
import com.example.data.model.ParcelPriority
import com.example.data.model.ParcelStatus
import com.example.data.model.Shift
import com.example.data.model.UiState
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.ParcelStatusBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.ShiftBadge
import com.example.ui.components.UserStatusBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.viewmodel.ManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboard(
    managerViewModel: ManagerViewModel,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateParcelDialog by remember { mutableStateOf(false) }
    var assigningParcel by remember { mutableStateOf<Parcel?>(null) }
    var inspectingShift by remember { mutableStateOf<Shift?>(null) }

    val parcelsState by managerViewModel.parcelsState.collectAsStateWithLifecycle()
    val ridersState by managerViewModel.ridersState.collectAsStateWithLifecycle()
    val shiftsState by managerViewModel.shiftsState.collectAsStateWithLifecycle()
    val liveTracking by managerViewModel.liveTrackingState.collectAsStateWithLifecycle()
    val actionMessage by managerViewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            managerViewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SAVE TRACK", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                        Text("Company Operations & Dispatch", fontSize = 11.sp, color = CyanAccent)
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("manager_sign_out_button")) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = Color(0xFFEF4444))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Navy900,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Dispatch") },
                    label = { Text("Dispatch") },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Inbox, contentDescription = "Parcels") },
                    label = { Text("Parcels") },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Riders & Shifts") },
                    label = { Text("Riders") },
                    colors = navigationItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Reports") },
                    label = { Text("Analytics") },
                    colors = navigationItemColors()
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showCreateParcelDialog = true },
                    containerColor = CyanAccent,
                    contentColor = Navy900,
                    modifier = Modifier.testTag("create_parcel_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Parcel")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> DispatchMapView(liveTracking = liveTracking, parcelsState = parcelsState)
                1 -> ParcelsListView(
                    parcelsState = parcelsState,
                    onAssignClick = { assigningParcel = it }
                )
                2 -> RidersAndShiftsView(
                    ridersState = ridersState,
                    shiftsState = shiftsState,
                    onApproveRider = { managerViewModel.approveRider(it) },
                    onInspectShift = { inspectingShift = it }
                )
                3 -> ManagerAnalyticsView(parcelsState = parcelsState, shiftsState = shiftsState)
            }
        }
    }

    if (showCreateParcelDialog) {
        CreateParcelDialog(
            onDismiss = { showCreateParcelDialog = false },
            onCreate = { newParcel ->
                managerViewModel.createParcel(newParcel) {
                    showCreateParcelDialog = false
                }
            }
        )
    }

    assigningParcel?.let { parcel ->
        AssignRiderDialog(
            parcel = parcel,
            ridersState = ridersState,
            onDismiss = { assigningParcel = null },
            onAssign = { riderId, riderName ->
                managerViewModel.assignParcelToRider(parcel.id, riderId, riderName)
                assigningParcel = null
            }
        )
    }

    inspectingShift?.let { shift ->
        ShiftSelfieInspectDialog(shift = shift, onDismiss = { inspectingShift = null })
    }
}

@Composable
private fun DispatchMapView(
    liveTracking: List<com.example.data.model.TrackingLog>,
    parcelsState: UiState<List<Parcel>>
) {
    if (liveTracking.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Map,
            title = "No Active Riders On Shift",
            description = "Active riders broadcasting real-time GPS telemetry from their shifts will be visible here on the live dispatch map."
        )
    } else {
        val firstRider = liveTracking.first()
        Column(modifier = Modifier.fillMaxSize()) {
            InteractiveMapView(
                riderLat = firstRider.latitude,
                riderLng = firstRider.longitude,
                riderName = firstRider.riderName,
                speed = firstRider.speed,
                accuracy = firstRider.accuracy,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
            )

            // Bottom roster of active broadcast cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(liveTracking) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Navy800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(log.riderName, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Lat: ${log.latitude}, Lng: ${log.longitude}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Text(
                                "${(log.speed * 3.6f).toInt()} km/h",
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelsListView(
    parcelsState: UiState<List<Parcel>>,
    onAssignClick: (Parcel) -> Unit
) {
    when (parcelsState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanAccent)
            }
        }
        is UiState.Error -> {
            EmptyStateView(
                icon = Icons.Default.Inbox,
                title = "Error Loading Parcels",
                description = parcelsState.message
            )
        }
        is UiState.Success -> {
            val parcels = parcelsState.data
            if (parcels.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inbox,
                    title = "No Parcels Registered",
                    description = "Start your operations by registering a real parcel using the (+) button below."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(parcels) { parcel ->
                        ParcelCardItem(parcel = parcel, onAssignClick = { onAssignClick(parcel) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelCardItem(
    parcel: Parcel,
    onAssignClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = Modifier.fillMaxWidth()
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

            Text(text = parcel.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "From: ${parcel.senderAddress}", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text(text = "To: ${parcel.deliveryAddress}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recipient: ${parcel.recipientName} (${parcel.recipientPhone})",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )

                if (parcel.riderId.isBlank()) {
                    Button(
                        onClick = onAssignClick,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("assign_rider_btn_${parcel.id}")
                    ) {
                        Text("Assign Rider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Rider: ${parcel.riderName}",
                        color = StatusGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RidersAndShiftsView(
    ridersState: UiState<List<User>>,
    shiftsState: UiState<List<Shift>>,
    onApproveRider: (String) -> Unit,
    onInspectShift: (Shift) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text("Delivery Riders Roster", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        when (ridersState) {
            is UiState.Loading -> CircularProgressIndicator(color = CyanAccent)
            is UiState.Error -> Text("Failed to load riders: ${ridersState.message}", color = Color.Red)
            is UiState.Success -> {
                val riders = ridersState.data
                if (riders.isEmpty()) {
                    Text("No registered delivery riders yet.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                } else {
                    riders.forEach { rider ->
                        RiderListItem(rider = rider, onApprove = { onApproveRider(rider.uid) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Shift Attendance & Selfie Audits", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        when (shiftsState) {
            is UiState.Loading -> CircularProgressIndicator(color = CyanAccent)
            is UiState.Error -> Text("Failed to load shifts: ${shiftsState.message}", color = Color.Red)
            is UiState.Success -> {
                val shifts = shiftsState.data
                if (shifts.isEmpty()) {
                    Text("No shifts recorded yet.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                } else {
                    shifts.forEach { shift ->
                        ShiftAttendanceCard(shift = shift, onClick = { onInspectShift(shift) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderListItem(rider: User, onApprove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(rider.name, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${rider.email} | ${rider.phoneNumber}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                if (rider.vehicleType.isNotBlank()) {
                    Text("Vehicle: ${rider.vehicleType} (${rider.licensePlate})", color = CyanAccent, fontSize = 11.sp)
                }
            }
            if (rider.status == UserStatus.PENDING.name) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Approve", color = Color.White, fontSize = 12.sp)
                }
            } else {
                UserStatusBadge(status = rider.status)
            }
        }
    }
}

@Composable
private fun ShiftAttendanceCard(shift: Shift, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decoded Thumbnail
            val bitmap = remember(shift.selfieBase64) {
                try {
                    val bytes = Base64.decode(shift.selfieBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .border(2.dp, CyanAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selfie",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(shift.riderName, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "Started: ${shift.startTime?.toDate()?.toString()?.take(19) ?: "Active"}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Text(shift.deviceInfo, color = Color(0xFF64748B), fontSize = 10.sp)
            }

            ShiftBadge(status = shift.status)
        }
    }
}

@Composable
private fun ShiftSelfieInspectDialog(shift: Shift, onDismiss: () -> Unit) {
    val bitmap = remember(shift.selfieBase64) {
        try {
            val bytes = Base64.decode(shift.selfieBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shift Attendance Verification", color = Color.White) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Rider Selfie",
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, CyanAccent, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Rider: ${shift.riderName}", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Shift ID: ${shift.id}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text("Device: ${shift.deviceInfo}", color = Color(0xFF94A3B8), fontSize = 11.sp)
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

@Composable
private fun CreateParcelDialog(
    onDismiss: () -> Unit,
    onCreate: (Parcel) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var senderAddress by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(ParcelPriority.NORMAL) }
    var destLat by remember { mutableStateOf("37.7749") }
    var destLng by remember { mutableStateOf("-122.4194") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Parcel", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Parcel Title / Description") },
                    colors = managerOutlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("parcel_title_input")
                )
                OutlinedTextField(
                    value = senderAddress,
                    onValueChange = { senderAddress = it },
                    label = { Text("Pickup Address") },
                    colors = managerOutlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("parcel_pickup_input")
                )
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("Delivery Destination Address") },
                    colors = managerOutlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("parcel_dest_input")
                )
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Recipient Name") },
                    colors = managerOutlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("parcel_recipient_name_input")
                )
                OutlinedTextField(
                    value = recipientPhone,
                    onValueChange = { recipientPhone = it },
                    label = { Text("Recipient Phone") },
                    colors = managerOutlinedColors(),
                    modifier = Modifier.fillMaxWidth().testTag("parcel_recipient_phone_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = destLat,
                        onValueChange = { destLat = it },
                        label = { Text("Dest Lat") },
                        colors = managerOutlinedColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = destLng,
                        onValueChange = { destLng = it },
                        label = { Text("Dest Lng") },
                        colors = managerOutlinedColors(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && deliveryAddress.isNotBlank() && recipientName.isNotBlank()) {
                        onCreate(
                            Parcel(
                                title = title,
                                senderAddress = senderAddress,
                                deliveryAddress = deliveryAddress,
                                recipientName = recipientName,
                                recipientPhone = recipientPhone,
                                priority = priority.name,
                                destLat = destLat.toDoubleOrNull() ?: 0.0,
                                destLng = destLng.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900),
                modifier = Modifier.testTag("submit_create_parcel_button")
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = Navy900
    )
}

@Composable
private fun AssignRiderDialog(
    parcel: Parcel,
    ridersState: UiState<List<User>>,
    onDismiss: () -> Unit,
    onAssign: (riderId: String, riderName: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Parcel to Rider", color = Color.White) },
        text = {
            when (ridersState) {
                is UiState.Loading -> CircularProgressIndicator(color = CyanAccent)
                is UiState.Error -> Text("Error loading riders: ${ridersState.message}", color = Color.Red)
                is UiState.Success -> {
                    val approvedRiders = ridersState.data.filter { it.status == UserStatus.ACTIVE.name }
                    if (approvedRiders.isEmpty()) {
                        Text("No active approved riders available. Please approve riders first.", color = Color(0xFF94A3B8))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(approvedRiders) { rider ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Navy800),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAssign(rider.uid, rider.name) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(rider.name, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Vehicle: ${rider.vehicleType}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Navy700)) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Navy900
    )
}

@Composable
private fun ManagerAnalyticsView(
    parcelsState: UiState<List<Parcel>>,
    shiftsState: UiState<List<Shift>>
) {
    val parcels = (parcelsState as? UiState.Success)?.data ?: emptyList()
    val shifts = (shiftsState as? UiState.Success)?.data ?: emptyList()

    val total = parcels.size
    val delivered = parcels.count { it.status == ParcelStatus.DELIVERED.name }
    val inTransit = parcels.count { it.status == ParcelStatus.IN_TRANSIT.name || it.status == ParcelStatus.PICKED_UP.name }
    val failed = parcels.count { it.status == ParcelStatus.FAILED.name }
    val activeShifts = shifts.count { it.status == "ACTIVE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Logistics Operational KPI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Total Parcels", "$total", Icons.Default.Inbox, CyanAccent, Modifier.weight(1f))
            MetricCard("Delivered", "$delivered", Icons.Default.Check, StatusGreen, Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("In Transit", "$inTransit", Icons.Default.LocalShipping, Color(0xFF38BDF8), Modifier.weight(1f))
            MetricCard("Failed / Exception", "$failed", Icons.Default.Analytics, StatusRed, Modifier.weight(1f))
        }

        MetricCard("Active Shifts", "$activeShifts", Icons.Default.Person, StatusGreen, Modifier.fillMaxWidth())
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(title, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun navigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Navy900,
    selectedTextColor = CyanAccent,
    indicatorColor = CyanAccent,
    unselectedIconColor = Color(0xFF64748B),
    unselectedTextColor = Color(0xFF64748B)
)

@Composable
private fun managerOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = Navy700,
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
