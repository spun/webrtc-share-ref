package com.spundev.webrtcshare.ui.screens.joinRequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spundev.webrtcshare.extensions.ModuleInstallException
import com.spundev.webrtcshare.utils.AvailabilityStatus
import com.spundev.webrtcshare.utils.MLKitManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JoinRequestViewModel @Inject constructor(
    val mlKitManager: MLKitManager,
) : ViewModel() {

    // The QR scanner state
    private val scannerState = MutableStateFlow<ScannerState?>(null)

    // Send events like "launch scanner ui" to the UI
    private val _screenEvents = MutableStateFlow<JoinRequestEvents?>(null)
    val screenEvents = _screenEvents.asStateFlow()

    init {
        // Initial availability check
        viewModelScope.launch {
            val availabilityStatus = mlKitManager.getBarcodeScannerAvailability()
            scannerState.value = when (availabilityStatus) {
                AvailabilityStatus.AlreadyAvailable,
                AvailabilityStatus.ReadyToDownload -> ScannerState.Ready

                is AvailabilityStatus.Unavailable -> ScannerState.Unavailable

                is AvailabilityStatus.Error -> {
                    Timber.w("initial availability: ${availabilityStatus.reason}")
                    ScannerState.Error
                }
            }
        }
    }

    val uiState: StateFlow<JoinRequestUiState> = scannerState
        // Wait for the initial scannerState to emit our uiState
        .filterNotNull()
        .mapLatest { JoinRequestUiState.Success(scannerState = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = JoinRequestUiState.Loading
        )

    fun scanRequest() {
        viewModelScope.launch {
            // Check availability again to see if something changed since our init check
            when (val availabilityStatus = mlKitManager.getBarcodeScannerAvailability()) {
                AvailabilityStatus.AlreadyAvailable -> {
                    // If the module is available, launch scanner and make sure
                    // that our scanner state is kept to Ready
                    scannerState.value = ScannerState.Ready
                    _screenEvents.value = JoinRequestEvents.LaunchScanner
                }

                AvailabilityStatus.ReadyToDownload -> {
                    // This will trigger an urgent install and launch the scanner
                    // when completed. The function we are calling will change
                    // the required states.
                    installScannerModuleAndLaunch()
                }

                is AvailabilityStatus.Unavailable -> {
                    // If the scanner module is Unavailable, make sure that the
                    // ui state is also Unavailable because the user shouldn't be
                    // able to trigger the installation from our unavailable UI.
                    scannerState.value = ScannerState.Unavailable
                }

                is AvailabilityStatus.Error -> {
                    Timber.w("launch availability: ${availabilityStatus.reason}")
                    scannerState.value = ScannerState.Error
                }
            }
        }
    }

    /**
     * Triggers the urgent module install and updates the uiState with the progress
     */
    private suspend fun installScannerModuleAndLaunch() {
        try {
            val progress = MutableStateFlow(0)
            scannerState.value = ScannerState.Installing(progress)
            mlKitManager.installScannerFlow()
                .onStart { emit(0) }
                .onEach { progress.value = it }
                .collect()
            scannerState.value = ScannerState.Ready
            // Wait until scanner is ready before launching
            // On one occasion, the call to startScan after installation failed
            // and the Toast said that the scanner was not ready.
            // For some reason, the module installation seems to return a
            // STATE_COMPLETED before it is really ready to be used.
            // We are just going to check availability a number of times before
            // calling startScan
            if (waitUntilFeatureAvailable()) {
                _screenEvents.value = JoinRequestEvents.LaunchScanner
            }
        } catch (e: ModuleInstallException) {
            Timber.w(e, "install error")
            scannerState.value = ScannerState.Error
        }
    }

    /**
     * Repeatedly checks if the scanner module is available.
     * We use this to get notified when the scanner switches from "installing" to "ready".
     * See installScannerModuleAndLaunch for more info.
     * @return true if the scanner became available, false if we reached the time/tries
     * limit and stopped checking.
     */
    private suspend fun waitUntilFeatureAvailable(
        maxAttempts: Int = 8,
        intervalMs: Long = 500L,
        timeoutMs: Long = 5_000L
    ): Boolean {
        Timber.d("Waiting for scanner to be ready")
        return withTimeoutOrNull(timeoutMs) {
            repeat(maxAttempts) { n ->
                val availability = mlKitManager.getBarcodeScannerAvailability()
                if (availability == AvailabilityStatus.AlreadyAvailable) {
                    Timber.d("- check $n: Ready")
                    return@withTimeoutOrNull true
                }
                Timber.d("- check $n: Not ready, waiting ${intervalMs}ms")
                delay(intervalMs)
            }
            false
        } ?: false
    }

    fun clearScanEvent() {
        _screenEvents.value = null
    }
}

sealed interface JoinRequestUiState {
    data object Loading : JoinRequestUiState
    data class Success(
        val scannerState: ScannerState
    ) : JoinRequestUiState
}

sealed interface ScannerState {
    data object Ready : ScannerState
    data class Installing(val progress: StateFlow<Int>) : ScannerState
    data object Unavailable : ScannerState
    data object Error : ScannerState
}

sealed interface JoinRequestEvents {
    data object LaunchScanner : JoinRequestEvents
}
