package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayReceiveStatusResponse(
    val amount: MoneroPayAmount,
    val complete: Boolean,
    val description: String,
    val createdAt: String,
    val transactions: List<MoneroPayTransaction>
)
