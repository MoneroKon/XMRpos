package org.monerokon.xmrpos.data.remote.moneroPayCallback.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentCallback(
    val amount: PaymentCallbackAmount,
    val complete: Boolean,
    val description: String,
    val created_at: String,
    val transaction: PaymentCallbackTransaction,
)
