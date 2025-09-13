package org.monerokon.xmrpos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO // Or your preferred Ktor engine (OkHttp, Android)
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking 
import kotlinx.serialization.json.Json
import org.monerokon.xmrpos.data.remote.auth.model.AuthTokenResponse
import org.monerokon.xmrpos.data.remote.backend.BackendRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPayCallback.MoneroPayCallbackManager
import org.monerokon.xmrpos.data.repository.AuthRepository
import org.monerokon.xmrpos.data.repository.BackendRepository
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.MoneroPayRepository
import org.monerokon.xmrpos.data.repository.TransactionRepository
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.text.isNotBlank

// Custom Qualifier for the main Ktor client used by BackendRepository
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainKtorClient

// Custom Qualifier for the Ktor client used ONLY for token refresh
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshKtorClient

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideBackendRepository(
        backendRemoteDataSource: BackendRemoteDataSource,
        @ApplicationScope applicationScope: CoroutineScope,
        dataStoreRepository: DataStoreRepository,
    ): BackendRepository {
        return BackendRepository(backendRemoteDataSource, applicationScope,dataStoreRepository)
    }


    @Provides
    @Singleton
    fun provideJsonSerializer(): Json {
        return Json {
            prettyPrint = true // Good for debugging, consider false for release
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    @Provides
    @RefreshKtorClient // Client specifically for token refresh; no Auth plugin, no dynamic base URL from DefaultRequest
    @Singleton
    fun provideRefreshKtorClient(
        json: Json
    ): HttpClient {
        return HttpClient(CIO) { // Or your preferred engine
            expectSuccess = false // Handle success/failure manually in refreshTokens block
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("KtorRefreshClient", message)
                    }
                }
                level = LogLevel.ALL
            }
            // NO DefaultRequest with base URL here. The full URL will be set in the refreshTokens call.
        }
    }

    @Provides
    @MainKtorClient // Main client for general API calls
    @Singleton
    fun provideMainKtorClient(
        json: Json,
        dataStoreRepository: DataStoreRepository,
        authRepository: AuthRepository,
        @RefreshKtorClient refreshClient: HttpClient // Inject the refresh client
    ): HttpClient {
        return HttpClient(CIO) { // Or OkHttp engine if you prefer/need its features
            expectSuccess = true // Ktor will throw exceptions for non-2xx responses by default

            // Default request configuration: applied to every request made by this client
            install(DefaultRequest) {
                contentType(ContentType.Application.Json) // Default content type for requests

                // THIS IS THE KEY for dynamic base URL:
                // This block is executed for each request.
                // It fetches the LATEST backend URL from DataStore.
                // runBlocking is used here because DefaultRequest builder is not suspendable.
                // This is generally acceptable if DataStore read is fast.
                val currentBackendUrl = runBlocking {
                    dataStoreRepository.getBackendInstanceUrl().firstOrNull()
                }

                if (currentBackendUrl != null && currentBackendUrl.isNotBlank()) {
                    url.takeFrom(currentBackendUrl) // Apply protocol, host, port from stored URL
                    // The path will be appended from the actual request (e.g., client.get("some/path"))
                } else {
                    // Handle case where URL is not yet set (e.g., before first login)
                    // Option A: Let requests fail (they shouldn't be made before login if URL is mandatory)
                    // Option B: Set a dummy URL that will clearly indicate an error
                    android.util.Log.w("MainKtorClient", "Backend URL not set in DefaultRequest!")
                    // url.takeFrom("http://url.not.set.yet.for.api") // Example dummy
                }
            }

            install(ContentNegotiation) {
                json(json) // Use the centrally configured Json instance
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("MainKtorClient", message)
                    }
                }
                level = LogLevel.ALL // Adjust log level as needed (BODY, HEADERS, etc.)
            }

            install(WebSockets)

            install(Auth) {
                bearer {
                    loadTokens {
                        // Load tokens from DataStore
                        val accessToken = dataStoreRepository.getBackendAccessToken().firstOrNull()
                        val refreshToken = dataStoreRepository.getBackendRefreshToken().firstOrNull()
                        if (accessToken != null && refreshToken != null) {
                            BearerTokens(accessToken, refreshToken)
                        } else {
                            null
                        }
                    }

                    refreshTokens {
                        // This block is executed when a 401 is received
                        android.util.Log.d("MainKtorClient", "Attempting to refresh tokens...")
                        val currentRefreshToken = oldTokens?.refreshToken ?: run {
                            android.util.Log.w("MainKtorClient", "No old refresh token found for refresh.")
                            return@refreshTokens null
                        }

                        // IMPORTANT: The refresh call itself needs the base URL
                        val backendUrlForRefresh = runBlocking { dataStoreRepository.getBackendInstanceUrl().firstOrNull() }
                        if (backendUrlForRefresh.isNullOrBlank()) {
                            android.util.Log.e("MainKtorClientAuth", "Cannot refresh token: Backend URL is not set.")
                            return@refreshTokens null // Cannot refresh without URL
                        }

                        // Use the @RefreshKtorClient which does NOT have the Auth plugin or DefaultRequest for base URL
                        val authTokenResponse: AuthTokenResponse? = try {
                            val response = refreshClient.post { // Use the dedicated refreshClient
                                url {
                                    takeFrom(backendUrlForRefresh) // Set the full base URL for this specific call
                                    appendPathSegments("auth", "refresh") // Your refresh token path
                                }
                                header(HttpHeaders.ContentType, ContentType.Application.Json)
                                setBody(mapOf("refresh_token" to currentRefreshToken)) // Adjust payload as needed
                            }
                            if (response.status.value in 200..299) {
                                response.body<AuthTokenResponse>()
                            } else {
                                android.util.Log.e("MainKtorClientAuth", "Token refresh failed with status: ${response.status}")
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainKtorClientAuth", "Token refresh exception", e)
                            null
                        }

                        if (authTokenResponse != null) {
                            dataStoreRepository.saveBackendAccessToken(authTokenResponse.access_token)
                            authTokenResponse.refresh_token.let { dataStoreRepository.saveBackendRefreshToken(it) }
                            BearerTokens(authTokenResponse.access_token, authTokenResponse.refresh_token)
                        } else {
                            // Failed to refresh, clear tokens to force re-login
                            authRepository.logout()
                            null
                        }
                    }

                    // Optional: Configure when to send tokens
                    // sendWithoutRequest { request ->
                    //    request.url.host == "your.api.host.from.datastore" // Only send for your API
                    // }
                }
            }
        }
    }
}

