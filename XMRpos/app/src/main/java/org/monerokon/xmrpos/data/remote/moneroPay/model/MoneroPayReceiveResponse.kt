package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayReceiveResponse(
    val address: String,
    val amount: Long,
    val description: String,
    val created_at: String,
)
