package org.monerokon.xmrpos.data.remote.exchangeRate

import org.monerokon.xmrpos.data.remote.exchangeRate.model.ExchangeRateResponse
import javax.inject.Inject

class ExchangeRateRemoteDataSource @Inject constructor(
    private val api: ExchangeRateApi
) {
    suspend fun fetchExchangeRates(fromSymbol: String, toSymbols: List<String>): ExchangeRateResponse {
        return api.fetchExchangeRates(fromSymbol, toSymbols.joinToString(","))
    }
}