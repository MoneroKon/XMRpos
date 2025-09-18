package org.monerokon.xmrpos.data.repository

import android.util.Log
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.monerokon.xmrpos.data.remote.backend.BackendRemoteDataSource
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionRequest
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendHealthResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendTransactionStatusUpdate
import org.monerokon.xmrpos.di.ApplicationScope
import org.monerokon.xmrpos.shared.DataResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@Singleton
class BackendRepository @Inject constructor(
    private val backendRemoteDataSource: BackendRemoteDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val dataStoreRepository: DataStoreRepository,
) {
    private val logTag = "BackendRepository"

    private var webSocketCollectionJob: Job? = null
    private var httpPollingJob: Job? = null
    private var currentWebSocketSession: DefaultClientWebSocketSession? = null

    var currentTransactionId: Int? = null

    private val _currentTransactionStatus =
        MutableStateFlow<BackendTransactionStatusUpdate?>(null)
    val currentTransactionStatus: StateFlow<BackendTransactionStatusUpdate?> =
        _currentTransactionStatus.asStateFlow()

    suspend fun health(): DataResult<BackendHealthResponse> {
        return backendRemoteDataSource.fetchHealth()
    }

    suspend fun createTransaction(request: BackendCreateTransactionRequest): DataResult<BackendCreateTransactionResponse> {
        return backendRemoteDataSource.createTransaction(request)
    }

    fun observeCurrentTransactionUpdates(transactionId: Int) {
        Log.i(logTag, "Request to observe transaction updates for ID: $transactionId")

        if (this.currentTransactionId == transactionId &&
            (webSocketCollectionJob?.isActive == true || httpPollingJob?.isActive == true)) {
            Log.d(logTag, "Already observing transaction ID $transactionId.")
            return
        }

        stopObservingTransactionUpdates()

        this.currentTransactionId = transactionId // Set the new current ID
        _currentTransactionStatus.value = null

        // Start WebSocket Observation
        webSocketCollectionJob = applicationScope.launch {
            Log.d(logTag, "Starting WebSocket collection for ID: $transactionId")
            try {
                backendRemoteDataSource.observeTransactionStatus(
                    id = transactionId,
                    onSessionEstablished = { session ->
                        this@BackendRepository.currentWebSocketSession = session
                        Log.i(logTag, "WebSocket session established for ID: $transactionId.")
                    }
                ).collect { update ->
                    Log.d(logTag, "[WS] Received update for ID $transactionId: $update")
                    // Check if the update is for the currently observed transaction
                    if (this@BackendRepository.currentTransactionId == transactionId) {
                        _currentTransactionStatus.value = update
                    }
                }
            } catch (e: CancellationException) {
                Log.i(logTag, "[WS] Observation for ID $transactionId was cancelled.")
            }
        }

        // Start HTTP Polling
        httpPollingJob = applicationScope.launch {
            Log.d(logTag, "Starting HTTP polling for ID: $transactionId")
            try {
                while (isActive) {
                    delay(dataStoreRepository.getBackendRequestInterval().first().seconds)

                    // Crucial check: are we still supposed to be polling for this transactionId?
                    if (this@BackendRepository.currentTransactionId != transactionId) {
                        Log.d(logTag, "[HTTP] currentTransactionId changed from $transactionId to ${this@BackendRepository.currentTransactionId}. Stopping this polling loop.")
                        break // Exit loop if the repository is now observing a different ID
                    }

                    Log.d(logTag, "[HTTP] Polling status for ID: $transactionId")
                    val fetchedStatus =
                        backendRemoteDataSource.fetchTransactionStatus(transactionId)

                    // Only update if we are still observing THIS specific transactionId
                    if (this@BackendRepository.currentTransactionId == transactionId) {
                        if (fetchedStatus is DataResult.Success) {
                            Log.d(logTag, "[HTTP] Received update for ID $transactionId: $fetchedStatus")
                            _currentTransactionStatus.value = fetchedStatus.data
                        } else {
                            Log.w(logTag, "[HTTP] Transaction $transactionId not found or error fetching via HTTP.")
                            if (webSocketCollectionJob?.isActive != true && _currentTransactionStatus.value != null) {
                                Log.i(logTag, "[HTTP] Transaction $transactionId seems to be gone/error, and WS is not active. Clearing status.")
                                _currentTransactionStatus.value = null
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.i(logTag, "[HTTP] Polling for ID $transactionId was cancelled.")
            }
        }
    }

    fun stopObservingTransactionUpdates() {
        val previousId = this.currentTransactionId

        // Check if there's anything to stop
        if (webSocketCollectionJob == null && httpPollingJob == null && currentWebSocketSession == null && previousId == null) {
            Log.d(logTag, "No active observation or context to stop.")
            return
        }

        Log.i(logTag, "Stopping transaction updates observation for ID: $previousId")

        // 1. Cancel the WebSocket collection Job
        if (webSocketCollectionJob?.isActive == true) {
            webSocketCollectionJob?.cancel("Stopping WebSocket observation due to new request or cleanup.")
            Log.d(logTag, "WebSocket collection job cancelled for ID: $previousId")
        }
        webSocketCollectionJob = null

        // 2. Cancel the HTTP Polling Job
        if (httpPollingJob?.isActive == true) {
            httpPollingJob?.cancel("Stopping HTTP polling due to new request or cleanup.")
            Log.d(logTag, "HTTP polling job cancelled for ID: $previousId")
        }
        httpPollingJob = null

        // 3. Close the WebSocket Session
        val sessionToClose = currentWebSocketSession
        currentWebSocketSession = null // Clear the reference immediately to prevent reuse

        if (sessionToClose?.isActive == true) {
            applicationScope.launch { // Ensure close is on a suitable dispatcher
                try {
                    Log.d(logTag, "Attempting to close WebSocket session for former TX ID: $previousId")
                    sessionToClose.close(CloseReason(CloseReason.Codes.NORMAL, "Client stopped observing"))
                    Log.i(logTag, "WebSocket session closed for former TX ID: $previousId")
                } catch (e: Exception) {
                    // Log the exception, but don't let it crash the app
                    Log.e(logTag, "Exception while closing WebSocket session for ID: $previousId", e)
                }
            }
        } else if (sessionToClose != null) {
            Log.d(logTag, "WebSocket session for ID: $previousId was already inactive or null.")
        }


        // 4. Reset the current transaction status
        if (previousId != null) {
            _currentTransactionStatus.value = null
            Log.d(logTag, "Current transaction status flow reset to null for ID: $previousId")
        }

        // 5. Clear the currentTransactionId
        this.currentTransactionId = null
        Log.d(logTag, "currentTransactionId reset.")
    }
}
