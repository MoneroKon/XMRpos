package org.monerokon.xmrpos.data.remote.backend.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendTransactionStatusUpdate(
    @SerialName("ID")
    val id: Int,

    @SerialName("CreatedAt")
    val createdAt: String,

    @SerialName("UpdatedAt")
    val updatedAt: String,

    @SerialName("DeletedAt")
    val deletedAt: String? = null,

    @SerialName("VendorID")
    val vendorId: Long, // Or Int

    @SerialName("PosID")
    val posId: Long, // Or Int

    @SerialName("Amount")
    val amount: Long,

    @SerialName("RequiredConfirmations")
    val requiredConfirmations: Int,

    @SerialName("Currency")
    val currency: String,

    @SerialName("AmountInCurrency")
    val amountInCurrency: Double,

    @SerialName("Description")
    val description: String? = null,

    @SerialName("SubAddress")
    val subAddress: String,

    @SerialName("Accepted")
    val accepted: Boolean,

    @SerialName("Confirmed")
    val confirmed: Boolean,

    @SerialName("SubTransactions")
    val subTransactions: List<SubTransaction> = emptyList()
)

@Serializable
data class SubTransaction(
    @SerialName("ID")
    val id: Long, // Or Int

    @SerialName("CreatedAt")
    val createdAt: String,

    @SerialName("UpdatedAt")
    val updatedAt: String,

    @SerialName("DeletedAt")
    val deletedAt: String? = null,

    @SerialName("TransactionID")
    val transactionId: Long, // Or Int, referencing the parent TransactionDetails.ID

    @SerialName("Amount")
    val amount: Long,

    @SerialName("Confirmations")
    val confirmations: Int,

    @SerialName("DoubleSpendSeen")
    val doubleSpendSeen: Boolean,

    @SerialName("Fee")
    val fee: Long,

    @SerialName("Height")
    val height: Long, // Or Int if block height won't exceed Int.MAX_VALUE

    @SerialName("Timestamp")
    val timestamp: String, // Keep as String, parse if needed

    @SerialName("TxHash")
    val txHash: String,

    @SerialName("UnlockTime")
    val unlockTime: Long,

    @SerialName("Locked")
    val locked: Boolean
)