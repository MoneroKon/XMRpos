package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayReceiveRequest(
    val amount: Long,
    val description: String,
    val callback_url: String?,
)
