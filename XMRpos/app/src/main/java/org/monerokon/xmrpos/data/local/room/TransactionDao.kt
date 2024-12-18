package org.monerokon.xmrpos.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.monerokon.xmrpos.data.local.room.model.Transaction

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end")
    suspend fun getTransactionsBetween(start: Long, end: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start")
    suspend fun getTransactionsAfter(start: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp <= :end")
    suspend fun getTransactionsBefore(end: Long): List<Transaction>
}