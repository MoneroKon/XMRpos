package org.monerokon.xmrpos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
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
import org.monerokon.xmrpos.data.repository.AuthRepository
import org.monerokon.xmrpos.data.repository.BackendRepository
import org.monerokon.xmrpos.data.repository.DataStoreRepository
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
        return HttpClient(CIO) {
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
        return HttpClient(CIO) {
            expectSuccess = true

            install(DefaultRequest) {
                contentType(ContentType.Application.Json)

                val currentBackendUrl = runBlocking {
                    dataStoreRepository.getBackendInstanceUrl().firstOrNull()
                }

                if (currentBackendUrl != null && currentBackendUrl.isNotBlank()) {
                    url.takeFrom(currentBackendUrl)
                } else {
                    android.util.Log.w("MainKtorClient", "Backend URL not set in DefaultRequest!")
                }
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("MainKtorClient", message)
                    }
                }
                level = LogLevel.ALL
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
                            return@refreshTokens null
                        }

                        val authTokenResponse: AuthTokenResponse? = try {
                            val response = refreshClient.post {
                                url {
                                    takeFrom(backendUrlForRefresh)
                                    appendPathSegments("auth", "refresh")
                                }
                                header(HttpHeaders.ContentType, ContentType.Application.Json)
                                setBody(mapOf("refresh_token" to currentRefreshToken))
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
                }
            }
        }
    }
}

