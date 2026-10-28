package com.example.data.repository

import android.content.Context
import com.example.data.model.DeliveryEvent
import com.example.data.model.Parcel
import com.example.data.model.ParcelStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ParcelRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    suspend fun createParcel(parcel: Parcel): String {
        val managerId = requireUserId()
        val docRef = db.collection("parcels").document()
        val generatedId = docRef.id
        val trackingNum = if (parcel.trackingNumber.isNotBlank()) parcel.trackingNumber else "ST-${System.currentTimeMillis().toString().takeLast(6)}"

        val payload = hashMapOf<String, Any>(
            "id" to generatedId,
            "trackingNumber" to trackingNum,
            "title" to parcel.title,
            "description" to parcel.description,
            "senderAddress" to parcel.senderAddress,
            "deliveryAddress" to parcel.deliveryAddress,
            "pickupLat" to parcel.pickupLat,
            "pickupLng" to parcel.pickupLng,
            "destLat" to parcel.destLat,
            "destLng" to parcel.destLng,
            "recipientName" to parcel.recipientName,
            "recipientPhone" to parcel.recipientPhone,
            "customerId" to parcel.customerId,
            "riderId" to parcel.riderId,
            "riderName" to parcel.riderName,
            "managerId" to managerId,
            "status" to if (parcel.riderId.isNotBlank()) ParcelStatus.ASSIGNED.name else ParcelStatus.CREATED.name,
            "priority" to parcel.priority,
            "notes" to parcel.notes,
            "failureReason" to "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            logDeliveryEvent(
                parcelId = generatedId,
                trackingNumber = trackingNum,
                eventType = if (parcel.riderId.isNotBlank()) "ASSIGNED" else "CREATED",
                description = "Parcel $trackingNum registered${if (parcel.riderName.isNotBlank()) " and assigned to ${parcel.riderName}" else ""}"
            )
            return generatedId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun assignRider(parcelId: String, riderId: String, riderName: String) {
        val docRef = db.collection("parcels").document(parcelId)
        try {
            docRef.update(
                mapOf(
                    "riderId" to riderId,
                    "riderName" to riderName,
                    "status" to ParcelStatus.ASSIGNED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logDeliveryEvent(
                parcelId = parcelId,
                trackingNumber = "",
                eventType = "ASSIGNED",
                description = "Assigned to rider $riderName"
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateParcelStatus(
        parcelId: String,
        newStatus: ParcelStatus,
        failureReason: String = "",
        lat: Double = 0.0,
        lng: Double = 0.0
    ) {
        val docRef = db.collection("parcels").document(parcelId)
        val updates = mutableMapOf<String, Any>(
            "status" to newStatus.name,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (failureReason.isNotBlank()) {
            updates["failureReason"] = failureReason
        }
        try {
            docRef.update(updates).await()
            logDeliveryEvent(
                parcelId = parcelId,
                trackingNumber = "",
                eventType = newStatus.name,
                description = when (newStatus) {
                    ParcelStatus.PICKED_UP -> "Rider picked up parcel from origin"
                    ParcelStatus.IN_TRANSIT -> "Parcel is in transit to destination"
                    ParcelStatus.ARRIVED -> "Rider arrived at destination address"
                    ParcelStatus.DELIVERED -> "Parcel successfully delivered to recipient"
                    ParcelStatus.FAILED -> "Delivery failed: $failureReason"
                    else -> "Status updated to ${newStatus.name}"
                },
                latitude = lat,
                longitude = lng
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    private suspend fun logDeliveryEvent(
        parcelId: String,
        trackingNumber: String,
        eventType: String,
        description: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        val user = Firebase.auth.currentUser ?: return
        val docRef = db.collection("delivery_events").document()
        val payload = hashMapOf<String, Any>(
            "id" to docRef.id,
            "parcelId" to parcelId,
            "trackingNumber" to trackingNumber,
            "actorId" to user.uid,
            "actorName" to (user.displayName ?: user.email ?: "Authorized User"),
            "eventType" to eventType,
            "description" to description,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    fun observeAllParcels(): Flow<List<Parcel>> = callbackFlow {
        val registration = db.collection("parcels").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "parcels")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Parcel::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeParcelsForRider(riderId: String): Flow<List<Parcel>> = callbackFlow {
        val query = db.collection("parcels").whereEqualTo("riderId", riderId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "parcels")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(Parcel::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    fun observeParcelByTrackingNumber(trackingNumber: String): Flow<Parcel?> = callbackFlow {
        val query = db.collection("parcels").whereEqualTo("trackingNumber", trackingNumber).limit(1)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "parcels")
                close(error)
                return@addSnapshotListener
            }
            val parcel = snapshot?.documents?.firstOrNull()?.toObject(Parcel::class.java)
            trySend(parcel)
        }
        awaitClose { registration.remove() }
    }

    fun observeDeliveryEvents(parcelId: String): Flow<List<DeliveryEvent>> = callbackFlow {
        val query = db.collection("delivery_events").whereEqualTo("parcelId", parcelId)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "delivery_events")
                close(error)
                return@addSnapshotListener
            }
            val events = snapshot?.documents?.mapNotNull { it.toObject(DeliveryEvent::class.java) }
                ?.sortedBy { it.timestamp?.toDate() } ?: emptyList()
            trySend(events)
        }
        awaitClose { registration.remove() }
    }
}
