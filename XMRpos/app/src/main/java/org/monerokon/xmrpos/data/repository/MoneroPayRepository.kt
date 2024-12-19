package org.monerokon.xmrpos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.local.room.model.Transaction
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import org.monerokon.xmrpos.data.remote.moneroPayCallback.MoneroPayCallbackManager
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallback

class MoneroPayRepository(
    private val moneroPayRemoteDataSource: MoneroPayRemoteDataSource,
    private val callbackManager: MoneroPayCallbackManager,
    private val transactionRepository: TransactionRepository,
    private val dataStoreRepository: DataStoreRepository
) {

    private val _paymentStatus = MutableStateFlow<PaymentCallback?>(null)
    val paymentStatus: StateFlow<PaymentCallback?> = _paymentStatus
    var currentCallbackUUID: String? = null

    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): MoneroPayReceiveResponse? {
        return withContext(Dispatchers.IO) {
            val response = moneroPayRemoteDataSource.startReceive(moneroPayReceiveRequest)
            response?.let {
                callbackManager.startListening { paymentCallback, fiatValue, callbackUUID ->
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        handlePaymentCallback(paymentCallback, fiatValue, callbackUUID)
                    }
                }
            }
            response
        }
    }

    fun updateCurrentCallbackUUID(callbackUUID: String?) {
        currentCallbackUUID = callbackUUID
    }

    private suspend fun handlePaymentCallback(paymentCallback: PaymentCallback, fiatValue: Double, callbackUUID: String) {
        if (currentCallbackUUID != callbackUUID) {
            return
        }

        val confValue = dataStoreRepository.getMoneroPayConfValue().first()
        if (confValue == "0-conf" && paymentCallback.transaction.confirmations != 0) {
            return
        } else if (confValue == "1-conf" && paymentCallback.transaction.confirmations != 1) {
            return
        } else if (confValue == "10-conf" && paymentCallback.transaction.confirmations != 10) {
            return
        }

        if (paymentCallback.amount.covered.total >= paymentCallback.amount.expected) {
            _paymentStatus.value = paymentCallback

            transactionRepository.insertTransaction(
                Transaction(
                    txId = paymentCallback.transaction.tx_hash,
                    xmrAmount = paymentCallback.amount.covered.total,
                    fiatValue = fiatValue,
                    timestamp = paymentCallback.transaction.timestamp
                )
            )
        }
    }


    fun stopReceive() {
        callbackManager.stopListening()
    }
}