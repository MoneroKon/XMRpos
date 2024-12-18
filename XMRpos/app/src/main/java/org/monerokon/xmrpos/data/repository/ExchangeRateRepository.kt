package org.monerokon.xmrpos.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.monerokon.xmrpos.data.local.datastore.DataStoreLocalDataSource
import org.monerokon.xmrpos.data.remote.exchangeRate.ExchangeRateRemoteDataSource
import org.monerokon.xmrpos.data.remote.exchangeRate.model.ExchangeRateResponse
import javax.inject.Inject

class ExchangeRateRepository @Inject constructor(
    private val exchangeRateRemoteDataSource: ExchangeRateRemoteDataSource,
    private val dataStoreLocalDataSource: DataStoreLocalDataSource // Or DataStoreLocalDataSource
) {

    private val logTag = "ExchangeRateRepository"

    // Fetch exchange rates using base currency from DataStore
    fun fetchPrimaryExchangeRate(): Flow<Result<ExchangeRateResponse>> {
        return dataStoreLocalDataSource.getPrimaryFiatCurrency().map { primaryFiatCurrency ->
            runCatching {
                Log.i(logTag, "Fetching exchange rates for primary fiat currency: $primaryFiatCurrency")
                exchangeRateRemoteDataSource.fetchExchangeRates("XMR",
                    listOf(primaryFiatCurrency)
                )
            }
        }
    }

    fun fetchReferenceExchangeRates(): Flow<Result<ExchangeRateResponse>> {
        return dataStoreLocalDataSource.getReferenceFiatCurrencies().map { referenceFiatCurrencies ->
            runCatching {
                Log.i(logTag, "Fetching exchange rates for reference fiat currency: $referenceFiatCurrencies")
                exchangeRateRemoteDataSource.fetchExchangeRates("XMR",
                    referenceFiatCurrencies
                )
            }
        }
    }

    // Fetch all exchange rates using primary and reference currencies
    fun fetchExchangeRates(): Flow<Result<ExchangeRateResponse>> {
        return dataStoreLocalDataSource.getFiatCurrencies().map { fiatCurrencies ->
            runCatching {
                Log.i(logTag, "Fetching exchange rates for primary and reference fiat currency: $fiatCurrencies")
                exchangeRateRemoteDataSource.fetchExchangeRates("XMR",
                    fiatCurrencies
                )
            }
        }
    }

    // get primary fiat currency from DataStore
    fun getPrimaryFiatCurrency(): Flow<String> {
        return dataStoreLocalDataSource.getPrimaryFiatCurrency()
    }

    // get reference fiat currencies from DataStore
    fun getReferenceFiatCurrencies(): Flow<List<String>> {
        return dataStoreLocalDataSource.getReferenceFiatCurrencies()
    }

}
