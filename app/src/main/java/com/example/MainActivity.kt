package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.repository.FirestoreProvider
import com.example.data.repository.ParcelRepository
import com.example.data.repository.ShiftRepository
import com.example.data.repository.TrackingRepository
import com.example.data.repository.UserRepository
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.customer.CustomerTrackingScreen
import com.example.ui.screens.manager.ManagerDashboard
import com.example.ui.screens.rider.RiderDashboard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SaveTrackTheme
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CustomerViewModel
import com.example.viewmodel.ManagerViewModel
import com.example.viewmodel.RiderViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val firestore = FirestoreProvider.get(this)
        val userRepository = UserRepository(firestore)
        val parcelRepository = ParcelRepository(firestore)
        val shiftRepository = ShiftRepository(firestore)
        val trackingRepository = TrackingRepository(firestore)

        setContent {
            SaveTrackTheme {
                val authViewModel: AuthViewModel = viewModel { AuthViewModel(userRepository) }
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                when (val state = authState) {
                    is AuthUiState.AuthenticatedWithProfile -> {
                        val user = state.user
                        when (user.role) {
                            UserRole.MANAGER.name -> {
                                val managerViewModel: ManagerViewModel = viewModel {
                                    ManagerViewModel(
                                        parcelRepository = parcelRepository,
                                        userRepository = userRepository,
                                        shiftRepository = shiftRepository,
                                        trackingRepository = trackingRepository
                                    )
                                }
                                ManagerDashboard(
                                    managerViewModel = managerViewModel,
                                    onSignOut = { authViewModel.signOut(this@MainActivity) }
                                )
                            }

                            UserRole.RIDER.name -> {
                                if (user.status == UserStatus.PENDING.name) {
                                    AuthScreen(
                                        authViewModel = authViewModel,
                                        authState = state
                                    )
                                } else {
                                    val riderViewModel: RiderViewModel = viewModel {
                                        RiderViewModel(
                                            parcelRepository = parcelRepository,
                                            shiftRepository = shiftRepository
                                        )
                                    }
                                    RiderDashboard(
                                        rider = user,
                                        riderViewModel = riderViewModel,
                                        onSignOut = { authViewModel.signOut(this@MainActivity) }
                                    )
                                }
                            }

                            UserRole.CUSTOMER.name -> {
                                val customerViewModel: CustomerViewModel = viewModel {
                                    CustomerViewModel(
                                        parcelRepository = parcelRepository,
                                        trackingRepository = trackingRepository
                                    )
                                }
                                CustomerTrackingScreen(
                                    customer = user,
                                    customerViewModel = customerViewModel,
                                    onSignOut = { authViewModel.signOut(this@MainActivity) }
                                )
                            }

                            else -> {
                                AuthScreen(
                                    authViewModel = authViewModel,
                                    authState = state
                                )
                            }
                        }
                    }

                    is AuthUiState.GuestVisit -> {
                        val guestUser = User(
                            uid = "guest_visitor",
                            name = "Guest Visitor",
                            email = "guest@savetrack.app",
                            role = UserRole.CUSTOMER.name
                        )
                        val customerViewModel: CustomerViewModel = viewModel {
                            CustomerViewModel(
                                parcelRepository = parcelRepository,
                                trackingRepository = trackingRepository
                            )
                        }
                        CustomerTrackingScreen(
                            customer = guestUser,
                            customerViewModel = customerViewModel,
                            onSignOut = { authViewModel.exitGuestMode() },
                            isGuest = true,
                            onSignInClick = { authViewModel.exitGuestMode() }
                        )
                    }

                    else -> {
                        AuthScreen(
                            authViewModel = authViewModel,
                            authState = state
                        )
                    }
                }
            }
        }
    }
}
