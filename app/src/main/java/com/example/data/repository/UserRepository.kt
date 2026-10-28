package com.example.data.repository

import android.content.Context
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(FirestoreProvider.get(context))

    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google")
    }

    fun observeUserProfile(userId: String): Flow<User?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            val user = snapshot?.toObject(User::class.java)
            trySend(user)
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveUserProfile(user: User) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role,
            "status" to user.status,
            "phoneNumber" to user.phoneNumber,
            "vehicleType" to user.vehicleType,
            "licensePlate" to user.licensePlate,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (user.createdAt == null) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeRiders(): Flow<List<User>> = callbackFlow {
        val query = db.collection("users").whereEqualTo("role", UserRole.RIDER.name)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "users")
                close(error)
                return@addSnapshotListener
            }
            val riders = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) } ?: emptyList()
            trySend(riders)
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateRiderStatus(riderId: String, status: UserStatus) {
        val docRef = db.collection("users").document(riderId)
        try {
            docRef.update("status", status.name, "updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
