package com.example.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.repository.UserRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object Unauthenticated : AuthUiState
    data object Loading : AuthUiState
    data object GuestVisit : AuthUiState
    data class AuthenticatedWithoutProfile(val firebaseUser: FirebaseUser) : AuthUiState
    data class AuthenticatedWithProfile(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentAuth()
    }

    private fun checkCurrentAuth() {
        val user = auth.currentUser
        if (user == null) {
            _uiState.value = AuthUiState.Unauthenticated
        } else {
            loadUserProfile(user.uid, user)
        }
    }

    private fun loadUserProfile(uid: String, firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            try {
                userRepository.observeUserProfile(uid).collect { profile ->
                    if (profile != null) {
                        _uiState.value = AuthUiState.AuthenticatedWithProfile(profile)
                    } else {
                        _uiState.value = AuthUiState.AuthenticatedWithoutProfile(firebaseUser)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading user profile", e)
                _uiState.value = AuthUiState.AuthenticatedWithoutProfile(firebaseUser)
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = AuthUiState.Loading
        val credentialManager = CredentialManager.create(context)
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error("Google Client ID missing: ${e.message}")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        loadUserProfile(firebaseUser.uid, firebaseUser)
                    } else {
                        _uiState.value = AuthUiState.Error("Sign in failed: No user returned")
                    }
                } else {
                    _uiState.value = AuthUiState.Error("Unexpected credential returned")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("AuthViewModel", "Google Sign-In cancelled: ${e.message}", e)
                _uiState.value = AuthUiState.Unauthenticated
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Sign-in failed")
            }
        }
    }

    fun completeProfileRegistration(
        name: String,
        role: UserRole,
        phoneNumber: String = "",
        vehicleType: String = "",
        licensePlate: String = ""
    ) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // If company manager, role is MANAGER; if rider, status is PENDING by default until approved
                val user = User(
                    uid = currentUser.uid,
                    name = name.ifBlank { currentUser.displayName ?: "Authorized User" },
                    email = currentUser.email ?: "",
                    role = role.name,
                    status = if (role == UserRole.RIDER) UserStatus.PENDING.name else UserStatus.ACTIVE.name,
                    phoneNumber = phoneNumber,
                    vehicleType = vehicleType,
                    licensePlate = licensePlate
                )
                userRepository.saveUserProfile(user)
                _uiState.value = AuthUiState.AuthenticatedWithProfile(user)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Failed to save profile: ${e.message}")
            }
        }
    }

    fun continueAsGuest() {
        _uiState.value = AuthUiState.GuestVisit
    }

    fun exitGuestMode() {
        _uiState.value = AuthUiState.Unauthenticated
    }

    fun signOut(context: Context) {
        val credentialManager = CredentialManager.create(context)
        auth.signOut()
        viewModelScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to clear credentials", e)
            } finally {
                _uiState.value = AuthUiState.Unauthenticated
            }
        }
    }
}
