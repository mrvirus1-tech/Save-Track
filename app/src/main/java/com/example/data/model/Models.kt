package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole {
    MANAGER,
    RIDER,
    CUSTOMER
}

enum class UserStatus {
    ACTIVE,
    PENDING,
    SUSPENDED
}

@IgnoreExtraProperties
data class User(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = UserRole.CUSTOMER.name,
    val status: String = UserStatus.ACTIVE.name,
    val phoneNumber: String = "",
    val vehicleType: String = "",
    val licensePlate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ParcelStatus {
    CREATED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED,
    DELIVERED,
    FAILED
}

enum class ParcelPriority {
    NORMAL,
    EXPRESS,
    URGENT
}

@IgnoreExtraProperties
data class Parcel(
    @DocumentId val id: String = "",
    val trackingNumber: String = "",
    val title: String = "",
    val description: String = "",
    val senderAddress: String = "",
    val deliveryAddress: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val recipientName: String = "",
    val recipientPhone: String = "",
    val customerId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val managerId: String = "",
    val status: String = ParcelStatus.CREATED.name,
    val priority: String = ParcelPriority.NORMAL.name,
    val notes: String = "",
    val failureReason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ShiftStatus {
    ACTIVE,
    COMPLETED
}

@IgnoreExtraProperties
data class Shift(
    @DocumentId val id: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val riderEmail: String = "",
    val selfieBase64: String = "",
    val status: String = ShiftStatus.ACTIVE.name,
    val lastLat: Double = 0.0,
    val lastLng: Double = 0.0,
    val deviceInfo: String = "",
    @ServerTimestamp val startTime: Timestamp? = null,
    @ServerTimestamp val endTime: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TrackingLog(
    @DocumentId val id: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f,
    val heading: Float = 0f,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DeliveryEvent(
    @DocumentId val id: String = "",
    val parcelId: String = "",
    val trackingNumber: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val eventType: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp val timestamp: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
