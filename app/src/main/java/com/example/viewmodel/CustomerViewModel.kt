package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeliveryEvent
import com.example.data.model.Parcel
import com.example.data.model.ParcelStatus
import com.example.data.model.TrackingLog
import com.example.data.repository.ParcelRepository
import com.example.data.repository.TrackingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerViewModel(
    private val parcelRepository: ParcelRepository,
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _searchedParcel = MutableStateFlow<Parcel?>(null)
    val searchedParcel: StateFlow<Parcel?> = _searchedParcel.asStateFlow()

    private val _deliveryEvents = MutableStateFlow<List<DeliveryEvent>>(emptyList())
    val deliveryEvents: StateFlow<List<DeliveryEvent>> = _deliveryEvents.asStateFlow()

    private val _riderLocation = MutableStateFlow<TrackingLog?>(null)
    val riderLocation: StateFlow<TrackingLog?> = _riderLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var trackingJob: Job? = null
    private var eventsJob: Job? = null
    private var riderLocationJob: Job? = null

    fun searchParcel(trackingNumber: String) {
        val cleanTracking = trackingNumber.trim()
        if (cleanTracking.isBlank()) {
            _errorMessage.value = "Please enter a valid tracking number"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        trackingJob?.cancel()
        eventsJob?.cancel()
        riderLocationJob?.cancel()

        trackingJob = viewModelScope.launch {
            try {
                parcelRepository.observeParcelByTrackingNumber(cleanTracking).collect { parcel ->
                    _isLoading.value = false
                    if (parcel == null) {
                        _searchedParcel.value = null
                        _errorMessage.value = "No parcel found with tracking number $cleanTracking"
                    } else {
                        _searchedParcel.value = parcel
                        _errorMessage.value = null
                        observeParcelEvents(parcel.id)

                        // Privacy rule: Share rider location ONLY when delivery is active
                        val isDelivering = parcel.status in listOf(
                            ParcelStatus.PICKED_UP.name,
                            ParcelStatus.IN_TRANSIT.name,
                            ParcelStatus.ARRIVED.name
                        )
                        if (isDelivering && parcel.riderId.isNotBlank()) {
                            observeRiderLocation(parcel.riderId)
                        } else {
                            _riderLocation.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = "Error searching parcel: ${e.message}"
            }
        }
    }

    private fun observeParcelEvents(parcelId: String) {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            parcelRepository.observeDeliveryEvents(parcelId).collect { events ->
                _deliveryEvents.value = events
            }
        }
    }

    private fun observeRiderLocation(riderId: String) {
        riderLocationJob?.cancel()
        riderLocationJob = viewModelScope.launch {
            trackingRepository.observeRiderLocation(riderId).collect { loc ->
                _riderLocation.value = loc
            }
        }
    }
}
