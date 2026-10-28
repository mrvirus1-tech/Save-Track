package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Parcel
import com.example.data.model.ParcelStatus
import com.example.data.model.Shift
import com.example.data.model.UiState
import com.example.data.repository.ParcelRepository
import com.example.data.repository.ShiftRepository
import com.example.service.LocationTrackingService
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RiderViewModel(
    private val parcelRepository: ParcelRepository,
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    private val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    val activeShiftState: StateFlow<Shift?> = shiftRepository.observeActiveShiftForRider(currentUserId)
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val assignedParcelsState: StateFlow<UiState<List<Parcel>>> = parcelRepository.observeParcelsForRider(currentUserId)
        .map<List<Parcel>, UiState<List<Parcel>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load assigned parcels")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _isStartingShift = MutableStateFlow(false)
    val isStartingShift: StateFlow<Boolean> = _isStartingShift.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startShiftWithSelfie(context: Context, selfieBase64: String, onComplete: () -> Unit) {
        _isStartingShift.value = true
        viewModelScope.launch {
            try {
                var lat = 0.0
                var lng = 0.0
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val lastLoc = fusedClient.lastLocation.await()
                    if (lastLoc != null) {
                        lat = lastLoc.latitude
                        lng = lastLoc.longitude
                    }
                } catch (e: Exception) {
                    // Location optional on simulator fallback
                }

                shiftRepository.startShift(selfieBase64, lat, lng)
                // Start Android foreground tracking service
                LocationTrackingService.start(context)
                _statusMessage.value = "Shift started successfully! Live GPS tracking active."
                onComplete()
            } catch (e: Exception) {
                _statusMessage.value = "Failed to start shift: ${e.message}"
            } finally {
                _isStartingShift.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun endShift(context: Context, shiftId: String) {
        viewModelScope.launch {
            try {
                var lat = 0.0
                var lng = 0.0
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                    val lastLoc = fusedClient.lastLocation.await()
                    if (lastLoc != null) {
                        lat = lastLoc.latitude
                        lng = lastLoc.longitude
                    }
                } catch (e: Exception) {
                    // Location optional
                }

                shiftRepository.endShift(shiftId, lat, lng)
                LocationTrackingService.stop(context)
                _statusMessage.value = "Shift ended. Tracking terminated."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to end shift: ${e.message}"
            }
        }
    }

    fun updateParcelStatus(parcelId: String, status: ParcelStatus, failureReason: String = "") {
        viewModelScope.launch {
            try {
                parcelRepository.updateParcelStatus(parcelId, status, failureReason)
                _statusMessage.value = "Parcel updated to ${status.name.replace('_', ' ')}"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to update parcel: ${e.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
