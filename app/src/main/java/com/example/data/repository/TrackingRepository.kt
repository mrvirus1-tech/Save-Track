package com.example.data.repository

import android.content.Context
import com.example.data.model.TrackingLog
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TrackingRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    suspend fun updateRiderLocation(
        riderId: String,
        riderName: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        speed: Float,
        heading: Float
    ) {
        val docRef = db.collection("tracking_logs").document(riderId)
        val payload = hashMapOf<String, Any>(
            "riderId" to riderId,
            "riderName" to riderName,
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracy" to accuracy,
            "speed" to speed,
            "heading" to heading,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    fun observeRiderLocation(riderId: String): Flow<TrackingLog?> = callbackFlow {
        if (riderId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("tracking_logs").document(riderId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            val log = snapshot?.toObject(TrackingLog::class.java)
            trySend(log)
        }
        awaitClose { registration.remove() }
    }

    fun observeAllRiderLocations(): Flow<List<TrackingLog>> = callbackFlow {
        val registration = db.collection("tracking_logs").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "tracking_logs")
                close(error)
                return@addSnapshotListener
            }
            val logs = snapshot?.documents?.mapNotNull { it.toObject(TrackingLog::class.java) } ?: emptyList()
            trySend(logs)
        }
        awaitClose { registration.remove() }
    }
}
