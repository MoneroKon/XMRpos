package org.monerokon.xmrpos.data.remote.backend

import android.system.Os
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionRequest
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendHealthResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendLoginRequest
import org.monerokon.xmrpos.data.remote.backend.model.BackendLoginResponse
import org.monerokon.xmrpos.data.remote.backend.model.BackendTransactionStatusUpdate
import org.monerokon.xmrpos.di.MainKtorClient
import org.monerokon.xmrpos.shared.DataResult
import javax.inject.Inject

class BackendRemoteDataSource @Inject constructor(
    @MainKtorClient private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun fetchHealth(): DataResult<BackendHealthResponse> {
        return try {
            val response = httpClient.get("misc/health").body<BackendHealthResponse>()
            DataResult.Success(response)
        } catch (e: Exception) {
            DataResult.Failure(message = e.message ?: "Unknown error")
        }
    }

    suspend fun createTransaction(backendCreateTransactionRequest: BackendCreateTransactionRequest): DataResult<BackendCreateTransactionResponse> {
        return try {
            val response = httpClient.post("pos/create-transaction") {
                setBody(backendCreateTransactionRequest)
            }
            DataResult.Success(response.body())
        } catch (e: Exception) {
            DataResult.Failure(message = e.message ?: "Unknown error")
        }
    }

    suspend fun fetchTransactionStatus(id: Int): DataResult<BackendTransactionStatusUpdate> {
        return try {
            val response = httpClient.get("pos/transaction/$id").body<BackendTransactionStatusUpdate>()
            DataResult.Success(response)
        } catch (e: Exception) {
            DataResult.Failure(message = e.message ?: "Unknown error")
        }
    }

    fun observeTransactionStatus(
        id: Int,
        onSessionEstablished: suspend (session: DefaultClientWebSocketSession) -> Unit
    ): Flow<BackendTransactionStatusUpdate> {
        return channelFlow {
            try {
                httpClient.webSocket(
                    method = HttpMethod.Get,
                    request = {
                        url {
                            protocol = URLProtocol.WSS
                            encodedPath = "/pos/ws/transaction?transaction_id=$id"
                        }
                    }
                ) {
                    Log.d("WebSocketDS", "WebSocket session attempting to establish for transaction ID: $id")
                    onSessionEstablished(this)
                    Log.d("WebSocketDS", "WebSocket onSessionEstablished called for transaction ID: $id")

                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> {
                                val rawJson = frame.readText()
                                Log.d("WebSocketDS", "Received raw JSON for transaction $id: $rawJson")
                                try {
                                    val update = json.decodeFromString<BackendTransactionStatusUpdate>(rawJson)
                                    send(update)
                                } catch (e: kotlinx.serialization.SerializationException) {
                                    Log.e("WebSocketDS", "Serialization error for transaction $id: $rawJson", e)
                                } catch (e: Exception) {
                                    Log.e("WebSocketDS", "Error processing text frame for transaction $id", e)
                                }
                            }
                            is Frame.Close -> {
                                val reason = frame.readReason()
                                val cause = reason?.let {
                                    Throwable("Server closed WS for transaction $id: Code ${it.code}, Message: ${it.message}")
                                } ?: Throwable("Server closed WS for transaction $id with no specific reason.")
                                Log.i("WebSocketDS", cause.message ?: "Server closed WebSocket.")
                                cancel()
                                break
                            }
                            is Frame.Binary -> Log.d("WebSocketDS", "Received Binary frame (unhandled) for transaction $id")
                            is Frame.Ping -> Log.d("WebSocketDS", "Received Ping frame for transaction $id")
                            is Frame.Pong -> Log.d("WebSocketDS", "Received Pong frame for transaction $id")
                            else -> Log.d("WebSocketDS", "Received other frame type (unhandled) for transaction $id")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocketDS", "WebSocket connection or processing error for transaction $id", e)
                close(e)
            } finally {
                if (!isClosedForSend) {
                    close()
                }
                Log.d("WebSocketDS", "WebSocket flow processing ended for transaction ID: $id")
            }
        }

    }
}