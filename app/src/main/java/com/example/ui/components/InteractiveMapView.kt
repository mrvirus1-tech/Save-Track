package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusGreen
import kotlin.math.roundToInt

@Composable
fun InteractiveMapView(
    riderLat: Double,
    riderLng: Double,
    riderName: String = "Rider",
    pickupLat: Double = 0.0,
    pickupLng: Double = 0.0,
    pickupAddress: String = "",
    destLat: Double = 0.0,
    destLng: Double = 0.0,
    deliveryAddress: String = "",
    speed: Float = 0f,
    accuracy: Float = 0f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Pulsing radar animation for live rider marker
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Navy900)
            .border(1.dp, Navy700, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(0.5f, 3.5f)
                    panOffset += pan
                }
            }
            .testTag("interactive_map_view")
    ) {
        // Custom Live Vector Map Grid with Route Vectors and Markers
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)

            // Draw GPS street grid lines
            val step = 60f * zoom
            var x = (center.x % step)
            while (x < size.width) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            var y = (center.y % step)
            while (y < size.height) {
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }

            // Reference baseline center
            val scaleFactor = 4000f * zoom
            val riderX = center.x
            val riderY = center.y

            // Destination waypoint relative to rider
            val hasDest = destLat != 0.0 && destLng != 0.0
            val destX = if (hasDest) riderX + ((destLng - riderLng) * scaleFactor).toFloat() else riderX + 160f * zoom
            val destY = if (hasDest) riderY - ((destLat - riderLat) * scaleFactor).toFloat() else riderY - 140f * zoom

            // Pickup waypoint relative to rider
            val hasPickup = pickupLat != 0.0 && pickupLng != 0.0
            val pickupX = if (hasPickup) riderX + ((pickupLng - riderLng) * scaleFactor).toFloat() else riderX - 140f * zoom
            val pickupY = if (hasPickup) riderY - ((pickupLat - riderLat) * scaleFactor).toFloat() else riderY + 120f * zoom

            // Draw Route Polylines
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // Pickup to Rider line
            drawLine(
                color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                start = Offset(pickupX, pickupY),
                end = Offset(riderX, riderY),
                strokeWidth = 3.dp.toPx(),
                pathEffect = dashedEffect
            )

            // Rider to Destination line (active tracking corridor)
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(riderX, riderY),
                end = Offset(destX, destY),
                strokeWidth = 4.dp.toPx()
            )

            // 1. Draw Pickup Marker (Blue Pin)
            drawCircle(
                color = Color(0xFF0284C7).copy(alpha = 0.3f),
                radius = 18f * zoom,
                center = Offset(pickupX, pickupY)
            )
            drawCircle(
                color = Color(0xFF0284C7),
                radius = 10f * zoom,
                center = Offset(pickupX, pickupY)
            )

            // 2. Draw Destination Marker (Green Flag/Pin)
            drawCircle(
                color = StatusGreen.copy(alpha = 0.3f),
                radius = 20f * zoom,
                center = Offset(destX, destY)
            )
            drawCircle(
                color = StatusGreen,
                radius = 11f * zoom,
                center = Offset(destX, destY)
            )

            // 3. Draw Rider Marker with Pulsing Radar Effect
            drawCircle(
                color = CyanAccent.copy(alpha = pulseAlpha),
                radius = pulseRadius * zoom,
                center = Offset(riderX, riderY)
            )
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                radius = 18f * zoom,
                center = Offset(riderX, riderY)
            )
            drawCircle(
                color = Color.White,
                radius = 10f * zoom,
                center = Offset(riderX, riderY)
            )
            drawCircle(
                color = CyanAccent,
                radius = 7f * zoom,
                center = Offset(riderX, riderY)
            )
        }

        // Live Telemetry Badge (Top Left)
        Surface(
            color = Navy800.copy(alpha = 0.9f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CyanAccent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE GPS: $riderName",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lat: ${"%.5f".format(riderLat)}, Lng: ${"%.5f".format(riderLng)}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                if (speed > 0f || accuracy > 0f) {
                    Text(
                        text = "Speed: ${(speed * 3.6f).roundToInt()} km/h | Acc: ${accuracy.roundToInt()}m",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Map Zoom Controls & Recenter Button (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { zoom = (zoom * 1.3f).coerceAtMost(3.5f) },
                modifier = Modifier
                    .size(36.dp)
                    .background(Navy800, CircleShape)
                    .border(1.dp, Navy700, CircleShape)
                    .testTag("map_zoom_in")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            IconButton(
                onClick = { zoom = (zoom / 1.3f).coerceAtLeast(0.5f) },
                modifier = Modifier
                    .size(36.dp)
                    .background(Navy800, CircleShape)
                    .border(1.dp, Navy700, CircleShape)
                    .testTag("map_zoom_out")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
            IconButton(
                onClick = {
                    panOffset = Offset.Zero
                    zoom = 1.0f
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Navy800, CircleShape)
                    .border(1.dp, CyanAccent, CircleShape)
                    .testTag("map_recenter")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on Rider", tint = CyanAccent)
            }
        }

        // External Navigation Action Banner (Bottom)
        if (destLat != 0.0 && destLng != 0.0) {
            Surface(
                color = Navy900.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Destination Target",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = deliveryAddress.ifBlank { "Delivery Coordinates (${"%.4f".format(destLat)}, ${"%.4f".format(destLng)})" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=$destLat,$destLng")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
                                context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_external_navigation_button")
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Navy900, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navigate", color = Navy900, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
