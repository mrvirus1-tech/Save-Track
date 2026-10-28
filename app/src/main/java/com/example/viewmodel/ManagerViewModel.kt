package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Parcel
import com.example.data.model.Shift
import com.example.data.model.TrackingLog
import com.example.data.model.UiState
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.data.repository.ParcelRepository
import com.example.data.repository.ShiftRepository
import com.example.data.repository.TrackingRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManagerViewModel(
    private val parcelRepository: ParcelRepository,
    private val userRepository: UserRepository,
    private val shiftRepository: ShiftRepository,
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    val parcelsState: StateFlow<UiState<List<Parcel>>> = parcelRepository.observeAllParcels()
        .map<List<Parcel>, UiState<List<Parcel>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to observe parcels")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val ridersState: StateFlow<UiState<List<User>>> = userRepository.observeRiders()
        .map<List<User>, UiState<List<User>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to observe riders")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val shiftsState: StateFlow<UiState<List<Shift>>> = shiftRepository.observeAllShifts()
        .map<List<Shift>, UiState<List<Shift>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to observe shifts")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val liveTrackingState: StateFlow<List<TrackingLog>> = trackingRepository.observeAllRiderLocations()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun createParcel(parcel: Parcel, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val newId = parcelRepository.createParcel(parcel)
                _actionMessage.value = "Parcel registered successfully (#${parcel.trackingNumber})"
                onSuccess(newId)
            } catch (e: Exception) {
                _actionMessage.value = "Error creating parcel: ${e.message}"
            }
        }
    }

    fun assignParcelToRider(parcelId: String, riderId: String, riderName: String) {
        viewModelScope.launch {
            try {
                parcelRepository.assignRider(parcelId, riderId, riderName)
                _actionMessage.value = "Assigned to $riderName"
            } catch (e: Exception) {
                _actionMessage.value = "Failed to assign rider: ${e.message}"
            }
        }
    }

    fun approveRider(riderId: String) {
        viewModelScope.launch {
            try {
                userRepository.updateRiderStatus(riderId, UserStatus.ACTIVE)
                _actionMessage.value = "Rider approved for delivery shifts"
            } catch (e: Exception) {
                _actionMessage.value = "Failed to approve rider: ${e.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
