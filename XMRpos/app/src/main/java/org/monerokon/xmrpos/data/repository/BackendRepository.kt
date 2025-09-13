package org.monerokon.xmrpos.data.repository

import android.util.Log
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.ktor.websocket.close
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.monerokon.xmrpos.data.remote.backend.BackendRemoteDataSource
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionRequest
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendHealthResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendTransactionStatusUpdate // Your DTO for WS messages
import org.monerokon.xmrpos.di.ApplicationScope
import org.monerokon.xmrpos.shared.DataResult // Your DataResult wrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendRepository @Inject constructor(
    private val backendRemoteDataSource: BackendRemoteDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val dataStoreRepository: DataStoreRepository,
) {

    private var currentObservingJob: Job? = null
    var currentTransactionId: Int? = null
    private var currentWebSocketSession: DefaultClientWebSocketSession? = null // To manually close

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
        Log.i("BackendRepository", "Request to observe transaction updates for ID: $transactionId")
        currentTransactionId = transactionId

        // Stop any existing observation first
        stopObservingTransactionUpdates()

        currentObservingJob = applicationScope.launch {
            Log.d("BackendRepository", "Starting WebSocket collection for ID: $transactionId")
            try {
                // Set to null or a "connecting" state before starting
                _currentTransactionStatus.value = null

                backendRemoteDataSource.observeTransactionStatus(
                    id = transactionId,
                    onSessionEstablished = { session ->
                        this@BackendRepository.currentWebSocketSession = session
                        Log.i("BackendRepository", "WebSocket session established for ID: $transactionId. Session: $session")
                    }
                ).collect { update ->
                    Log.d("BackendRepository", "Received update for ID $transactionId: $update")
                    _currentTransactionStatus.value = update
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.i("BackendRepository", "Observation for ID $transactionId was cancelled.", e)
            } catch (e: Exception) {
                Log.e("BackendRepository", "Error observing transaction $transactionId", e)
                _currentTransactionStatus.value = null
            } finally {
                Log.d("BackendRepository", "Collection coroutine finished for ID: $transactionId")
                if (this@BackendRepository.currentWebSocketSession?.isActive == false) {
                    this@BackendRepository.currentWebSocketSession = null
                }
            }
        }
    }

    /**
     * Stops any active transaction status observation and closes the WebSocket.
     */
    fun stopObservingTransactionUpdates() {
        if (currentObservingJob == null && currentWebSocketSession == null) {
            Log.d("BackendRepository", "No active observation to stop.")
            return
        }
        Log.i("BackendRepository", "Stopping transaction updates observation.")

        currentObservingJob?.cancel("Stopping observation")
        currentObservingJob = null

        val sessionToClose = currentWebSocketSession
        currentWebSocketSession = null // Clear ref immediately

        if (sessionToClose?.isActive == true) {
            applicationScope.launch { // Close on a background thread
                try {
                    sessionToClose.close(CloseReason(CloseReason.Codes.NORMAL, "Client stopped observing"))
                    Log.d("BackendRepository", "WebSocket session closed.")
                } catch (e: Exception) {
                    Log.e("BackendRepository", "Error closing WebSocket session", e)
                }
            }
        }
        // Reset status to null to indicate no active observation
        _currentTransactionStatus.value = null
    }
}
