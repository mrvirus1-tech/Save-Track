package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParcelPriority
import com.example.data.model.ParcelStatus
import com.example.data.model.ShiftStatus
import com.example.data.model.UserStatus
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.Navy900
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed

@Composable
fun ParcelStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        ParcelStatus.CREATED.name -> Color(0xFF334155) to Color.White
        ParcelStatus.ASSIGNED.name -> StatusBlue.copy(alpha = 0.2f) to StatusBlue
        ParcelStatus.PICKED_UP.name -> StatusAmber.copy(alpha = 0.2f) to StatusAmber
        ParcelStatus.IN_TRANSIT.name -> CyanAccent.copy(alpha = 0.2f) to CyanAccent
        ParcelStatus.ARRIVED.name -> StatusGreen.copy(alpha = 0.2f) to StatusGreen
        ParcelStatus.DELIVERED.name -> StatusGreen to Color.White
        ParcelStatus.FAILED.name -> StatusRed to Color.White
        else -> Color.Gray.copy(alpha = 0.2f) to Color.White
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.replace('_', ' '),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bgColor, textColor) = when (priority) {
        ParcelPriority.URGENT.name -> StatusRed.copy(alpha = 0.15f) to StatusRed
        ParcelPriority.EXPRESS.name -> StatusAmber.copy(alpha = 0.15f) to StatusAmber
        else -> Color(0xFF64748B).copy(alpha = 0.15f) to Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ShiftBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        ShiftStatus.ACTIVE.name -> StatusGreen.copy(alpha = 0.2f) to StatusGreen
        else -> Color(0xFF64748B).copy(alpha = 0.2f) to Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (status == ShiftStatus.ACTIVE.name) "ON DUTY" else "OFF DUTY",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UserStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        UserStatus.ACTIVE.name -> StatusGreen.copy(alpha = 0.2f) to StatusGreen
        UserStatus.PENDING.name -> StatusAmber.copy(alpha = 0.2f) to StatusAmber
        else -> StatusRed.copy(alpha = 0.2f) to StatusRed
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
