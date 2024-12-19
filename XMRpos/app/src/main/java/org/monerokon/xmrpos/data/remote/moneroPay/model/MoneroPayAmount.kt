package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayAmount(
    val expected: Long,
    val covered: MoneroPayCovered
)
