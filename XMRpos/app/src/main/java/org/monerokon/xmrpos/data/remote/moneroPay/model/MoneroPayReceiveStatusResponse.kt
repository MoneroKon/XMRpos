package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayReceiveStatusResponse(
    val amount: MoneroPayAmount,
    val complete: Boolean,
    val description: String,
    val created_at: String,
    val transactions: List<MoneroPayTransaction>?
)
