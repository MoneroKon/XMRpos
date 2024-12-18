package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayTransaction(
    val amount: Long,
    val confirmations: Int,
    val doubleSpendSeen: Boolean,
    val fee: Long,
    val height: Int,
    val timestamp: String,
    val txHash: String,
    val unlockTime: Int,
    val locked: Boolean
)
