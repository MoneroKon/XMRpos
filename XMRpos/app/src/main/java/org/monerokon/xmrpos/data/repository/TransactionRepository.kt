package org.monerokon.xmrpos.data.repository

import org.monerokon.xmrpos.data.local.room.TransactionDao
import org.monerokon.xmrpos.data.local.room.model.Transaction

class TransactionRepository(private val transactionDao: TransactionDao) {
    suspend fun insertTransaction(transaction: Transaction) = transactionDao.insertTransaction(transaction)

    suspend fun getAllTransactions(): List<Transaction> = transactionDao.getAllTransactions()

    suspend fun getTransactionById(id: Int): Transaction = transactionDao.getTransactionById(id)

    suspend fun deleteTransactionById(id: Int) = transactionDao.deleteTransactionById(id)

    suspend fun deleteAllTransactions() = transactionDao.deleteAllTransactions()

    suspend fun getTransactionsBetween(start: Long, end: Long): List<Transaction> = transactionDao.getTransactionsBetween(start, end)

    suspend fun getTransactionsAfter(start: Long): List<Transaction> = transactionDao.getTransactionsAfter(start)

    suspend fun getTransactionsBefore(end: Long): List<Transaction> = transactionDao.getTransactionsBefore(end)
}