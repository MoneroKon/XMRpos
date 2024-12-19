package org.monerokon.xmrpos.data.remote.moneroPayCallback.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentCallbackAmount(
    val expected: Long,
    val covered: PaymentCallbackCovered
)
