package org.monerokon.xmrpos.data.remote.moneroPayCallback.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentCallbackCovered(
    val total: Long,
    val unlocked: Long
)
