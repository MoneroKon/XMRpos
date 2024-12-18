package org.monerokon.xmrpos.data.remote.moneroPay

import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import javax.inject.Inject

class MoneroPayRemoteDataSource @Inject constructor(
    private val api: MoneroPayApi
) {
    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): MoneroPayReceiveResponse {
        return api.startReceive(moneroPayReceiveRequest)
    }
}