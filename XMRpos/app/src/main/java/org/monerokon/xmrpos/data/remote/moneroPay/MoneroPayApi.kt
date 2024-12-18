package org.monerokon.xmrpos.data.remote.moneroPay

import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveStatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MoneroPayApi {
    @POST("receive")
    suspend fun startReceive(
        @Body requestData: MoneroPayReceiveRequest
    ): MoneroPayReceiveResponse

    @GET("receive/{address}")
    suspend fun fetchReceiveStatus(
        @Path("address") address: String
    ): MoneroPayReceiveStatusResponse
}