package org.monerokon.xmrpos.data.remote.exchangeRate

import org.monerokon.xmrpos.data.remote.exchangeRate.model.ExchangeRateResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
    @GET("price")
    suspend fun fetchExchangeRates(
        @Query("fsym") fromSymbol: String,
        @Query("tsyms") toSymbols: String
    ): ExchangeRateResponse
}