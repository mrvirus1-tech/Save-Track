package com.example.ui.screens.customer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DeliveryEvent
import com.example.data.model.Parcel
import com.example.data.model.ParcelStatus
import com.example.data.model.User
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.ParcelStatusBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusGreen
import com.example.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTrackingScreen(
    customer: User,
    customerViewModel: CustomerViewModel,
    onSignOut: () -> Unit,
    isGuest: Boolean = false,
    onSignInClick: (() -> Unit)? = null
) {
    var searchInput by remember { mutableStateOf("") }

    val searchedParcel by customerViewModel.searchedParcel.collectAsStateWithLifecycle()
    val deliveryEvents by customerViewModel.deliveryEvents.collectAsStateWithLifecycle()
    val riderLocation by customerViewModel.riderLocation.collectAsStateWithLifecycle()
    val isLoading by customerViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by customerViewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SAVE TRACK", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text(
                            text = if (isGuest) "Guest Tracking Portal" else "Live Package Tracking",
                            fontSize = 11.sp,
                            color = CyanAccent
                        )
                    }
                },
                actions = {
                    if (isGuest && onSignInClick != null) {
                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("guest_top_signin_button")
                        ) {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("customer_sign_out_button")) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = if (isGuest) "Exit Guest Mode" else "Sign Out",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Guest Mode Notice Banner
            if (isGuest) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2342)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Visiting as a Guest", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(
                                "Track live parcels anytime. Sign in to register as a delivery rider or manage company dispatches.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
            // Search Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy800),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Track Your Package", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Enter Tracking Number (e.g. ST-...)") },
                            colors = customerOutlinedColors(),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_tracking_input")
                        )
                        Button(
                            onClick = { customerViewModel.searchParcel(searchInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Navy900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("customer_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanAccent)
                }
            } else if (errorMessage != null) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "Tracking Lookup",
                    description = errorMessage ?: ""
                )
            } else if (searchedParcel != null) {
                val parcel = searchedParcel!!
                ParcelDetailCard(parcel = parcel)

                Spacer(modifier = Modifier.height(16.dp))

                // Live Rider Map (visible only during active delivery window)
                if (riderLocation != null && parcel.status in listOf(ParcelStatus.PICKED_UP.name, ParcelStatus.IN_TRANSIT.name, ParcelStatus.ARRIVED.name)) {
                    Text("Live Rider Location", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        InteractiveMapView(
                            riderLat = riderLocation!!.latitude,
                            riderLng = riderLocation!!.longitude,
                            riderName = riderLocation!!.riderName,
                            pickupLat = parcel.pickupLat,
                            pickupLng = parcel.pickupLng,
                            destLat = parcel.destLat,
                            destLng = parcel.destLng,
                            deliveryAddress = parcel.deliveryAddress,
                            speed = riderLocation!!.speed,
                            accuracy = riderLocation!!.accuracy,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Delivery Status Stepper & Event Timeline
                DeliveryTimelineCard(events = deliveryEvents, currentStatus = parcel.status)
            } else {
                EmptyStateView(
                    icon = Icons.Default.LocalShipping,
                    title = "Enter a Tracking Number",
                    description = "Enter your unique SAVE TRACK package ID to view live delivery status, real-time rider GPS, and timeline milestones."
                )
            }
        }
    }
}

@Composable
private fun ParcelDetailCard(parcel: Parcel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${parcel.trackingNumber}",
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 17.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PriorityBadge(priority = parcel.priority)
                    ParcelStatusBadge(status = parcel.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(parcel.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(10.dp))
            Text("Origin: ${parcel.senderAddress}", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text("Destination: ${parcel.deliveryAddress}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            if (parcel.riderName.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Assigned Driver: ${parcel.riderName}", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DeliveryTimelineCard(
    events: List<DeliveryEvent>,
    currentStatus: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Milestones", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(14.dp))

            val milestones = listOf(
                "CREATED" to "Order Created",
                "ASSIGNED" to "Rider Assigned",
                "PICKED_UP" to "Parcel Picked Up",
                "IN_TRANSIT" to "Out for Delivery",
                "DELIVERED" to "Delivered Successfully"
            )

            milestones.forEachIndexed { index, (statusKey, label) ->
                val isCompleted = isStatusReached(currentStatus, statusKey)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) StatusGreen else Navy700),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        color = if (isCompleted) Color.White else Color(0xFF64748B),
                        fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
                if (index < milestones.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 11.dp)
                            .width(2.dp)
                            .height(18.dp)
                            .background(Navy700)
                    )
                }
            }

            if (events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Event Audit Log", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                events.forEach { event ->
                    Text(
                        text = "• ${event.description} (${event.timestamp?.toDate()?.toString()?.take(19) ?: ""})",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun isStatusReached(currentStatus: String, targetStatus: String): Boolean {
    val hierarchy = listOf("CREATED", "ASSIGNED", "PICKED_UP", "IN_TRANSIT", "ARRIVED", "DELIVERED")
    val currentIndex = hierarchy.indexOf(currentStatus)
    val targetIndex = hierarchy.indexOf(targetStatus)
    return currentIndex >= 0 && targetIndex >= 0 && currentIndex >= targetIndex
}

@Composable
private fun customerOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = Navy700,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
