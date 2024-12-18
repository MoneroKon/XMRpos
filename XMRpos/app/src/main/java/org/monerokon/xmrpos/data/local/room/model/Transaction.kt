package org.monerokon.xmrpos.data.local.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val txId: String,
    val xmrAmount: Long,
    val fiatValue: Double,
    val timestamp: String
)