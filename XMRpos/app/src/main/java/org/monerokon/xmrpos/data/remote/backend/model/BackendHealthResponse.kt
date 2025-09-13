package org.monerokon.xmrpos.data.remote.backend.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendHealthResponse(
    val status: Int,
    val services: TopLevelServices
)

@Serializable
data class TopLevelServices(
    val postgresql: Boolean,
    @SerialName("MoneroPay") // Use @SerialName if the Kotlin property name differs from JSON key
    val moneroPay: MoneroPayStatus
)

@Serializable
data class MoneroPayStatus(
    val status: Int,
    val services: MoneroPayServices
)

@Serializable
data class MoneroPayServices(
    @SerialName("walletrpc") // Example: if you wanted to name the Kotlin property 'walletRpc'
    val walletRpc: Boolean, // Changed property name for demonstration, can be 'walletrpc'
    val postgresql: Boolean
)

