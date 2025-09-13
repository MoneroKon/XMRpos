package org.monerokon.xmrpos.data.remote.backend.model

import kotlinx.serialization.Serializable
@Serializable
data class BackendCreateTransactionRequest (
    val amount: Long,
    val description: String,
    val amount_in_currency: Double,
    val currency: String,
    val required_confirmations: Int,
)