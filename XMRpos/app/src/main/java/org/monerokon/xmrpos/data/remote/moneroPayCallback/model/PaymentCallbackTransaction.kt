package org.monerokon.xmrpos.data.remote.moneroPayCallback.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentCallbackTransaction(
    val amount: Long,
    val confirmations: Int,
    val double_spend_seen: Boolean,
    val fee: Long,
    val height: Int,
    val timestamp: String,
    val tx_hash: String,
    val unlock_time: Int,
    val locked: Boolean
)
