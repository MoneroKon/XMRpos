package org.monerokon.xmrpos.data.remote.backend.model

import kotlinx.serialization.Serializable
@Serializable
data class BackendCreateTransactionResponse(
    val id: Int,
    val address: String,
)