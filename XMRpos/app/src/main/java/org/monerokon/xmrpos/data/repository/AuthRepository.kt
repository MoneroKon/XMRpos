package org.monerokon.xmrpos.data.repository

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO // Or your preferred engine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.monerokon.xmrpos.data.remote.auth.model.AuthLoginRequest
import org.monerokon.xmrpos.data.remote.auth.model.AuthTokenResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val jsonSerializer: Json,
    private val dataStoreRepository: DataStoreRepository
) {

    private val TAG = "AuthRepository"

    suspend fun login(instanceUrl: String, vendorID: Int, username: String, password: String): Result<Unit> {
        Log.d(TAG, "Attempting login for user: ${username} at URL: $instanceUrl")

        // Create a temporary Ktor client specifically for this login attempt
        val loginClient = HttpClient(CIO) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(jsonSerializer)
            }
            install(Logging) {
                logger = object : Logger { override fun log(message: String) { Log.d("LoginClient", message) } }
                level = LogLevel.ALL
            }
        }

        return try {
            val response: AuthTokenResponse = loginClient.post {
                url {
                    takeFrom(instanceUrl)
                    if (protocol == URLProtocol.HTTP && !instanceUrl.startsWith("http://") && !instanceUrl.startsWith("https://")) {
                        protocol = URLProtocol.HTTP
                    }
                    encodedPath = "/auth/login-pos"
                }
                contentType(ContentType.Application.Json)
                setBody(AuthLoginRequest(vendorID, username, password))
            }.body()

            dataStoreRepository.saveBackendInstanceUrl(instanceUrl)
            dataStoreRepository.saveBackendAccessToken(response.access_token)
            dataStoreRepository.saveBackendRefreshToken(response.refresh_token)

            Log.i(TAG, "Login successful to $instanceUrl. Tokens and URL stored.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed for URL $instanceUrl", e)
            Result.failure(e)
        } finally {
            loginClient.close()
        }
    }

    suspend fun logout() {
        Log.d(TAG, "Attempting logout.")

        dataStoreRepository.clearDataStore()

        Log.i(TAG, "DataStore cleared. Logout complete.")
    }

    fun isLoggedIn(): Flow<Boolean> {
        return dataStoreRepository.getBackendAccessToken().map {it.isNotBlank() }
    }

    fun observeAccessToken(): Flow<String?> = dataStoreRepository.getBackendAccessToken()
    fun observeRefreshToken(): Flow<String?> = dataStoreRepository.getBackendRefreshToken()
    fun observeBackendUrl(): Flow<String?> = dataStoreRepository.getBackendInstanceUrl()
}
