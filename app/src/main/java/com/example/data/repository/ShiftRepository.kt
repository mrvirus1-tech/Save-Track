package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.model.Shift
import com.example.data.model.ShiftStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ShiftRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    suspend fun startShift(selfieBase64: String, lat: Double, lng: Double): String {
        val user = Firebase.auth.currentUser ?: throw IllegalStateException("User must be signed in")
        val docRef = db.collection("shifts").document()
        val shiftId = docRef.id

        val payload = hashMapOf<String, Any>(
            "id" to shiftId,
            "riderId" to user.uid,
            "riderName" to (user.displayName ?: user.email ?: "Rider"),
            "riderEmail" to (user.email ?: ""),
            "selfieBase64" to selfieBase64,
            "status" to ShiftStatus.ACTIVE.name,
            "lastLat" to lat,
            "lastLng" to lng,
            "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
            "startTime" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return shiftId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun endShift(shiftId: String, lat: Double, lng: Double) {
        val docRef = db.collection("shifts").document(shiftId)
        try {
            docRef.update(
                mapOf(
                    "status" to ShiftStatus.COMPLETED.name,
                    "endTime" to FieldValue.serverTimestamp(),
                    "lastLat" to lat,
                    "lastLng" to lng,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeActiveShiftForRider(riderId: String): Flow<Shift?> = callbackFlow {
        val query = db.collection("shifts")
            .whereEqualTo("riderId", riderId)
            .whereEqualTo("status", ShiftStatus.ACTIVE.name)
            .limit(1)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "shifts")
                close(error)
                return@addSnapshotListener
            }
            val shift = snapshot?.documents?.firstOrNull()?.toObject(Shift::class.java)
            trySend(shift)
        }
        awaitClose { registration.remove() }
    }

    fun observeAllShifts(): Flow<List<Shift>> = callbackFlow {
        val registration = db.collection("shifts").addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "shifts")
                close(error)
                return@addSnapshotListener
            }
            val shifts = snapshot?.documents?.mapNotNull { it.toObject(Shift::class.java) }
                ?.sortedByDescending { it.startTime?.toDate() } ?: emptyList()
            trySend(shifts)
        }
        awaitClose { registration.remove() }
    }
}
