package org.monerokon.xmrpos.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.monerokon.xmrpos.data.local.datastore.DataStoreLocalDataSource
import org.monerokon.xmrpos.data.remote.exchangeRate.ExchangeRateRemoteDataSource
import org.monerokon.xmrpos.data.remote.exchangeRate.model.ExchangeRateResponse
import org.monerokon.xmrpos.shared.DataResult
import javax.inject.Inject
import kotlin.math.abs

private const val PRIMARY_REFRESH_INTERVAL_MS = 60_000L      // 1 minute
private const val FULL_REFRESH_INTERVAL_MS = 1_800_000L      // 30 minutes

class ExchangeRateRepository @Inject constructor(
    private val exchangeRateRemoteDataSource: ExchangeRateRemoteDataSource,
    private val dataStoreLocalDataSource: DataStoreLocalDataSource // Or DataStoreLocalDataSource
) {

    private val logTag = "ExchangeRateRepository"

    fun fetchPrimaryExchangeRate(): Flow<DataResult<ExchangeRateResponse>> = flow {
        val primaryFiatCurrency = dataStoreLocalDataSource.getPrimaryFiatCurrency().first()
        if (primaryFiatCurrency.isBlank()) {
            emit(DataResult.Success(emptyMap()))
            return@flow
        }

        val cachedRates = dataStoreLocalDataSource.getCachedExchangeRates().first()
        val lastPrimaryRefresh = dataStoreLocalDataSource.getPrimaryExchangeRateLastUpdated().first()
        val now = System.currentTimeMillis()

        val cachedRate = cachedRates[primaryFiatCurrency]
        val shouldRefresh = cachedRate == null || isOlderThan(now, lastPrimaryRefresh, PRIMARY_REFRESH_INTERVAL_MS)

        if (!shouldRefresh) {
            emit(DataResult.Success(mapOf(primaryFiatCurrency to cachedRate!!)))
            return@flow
        }

        Log.i(logTag, "Refreshing primary fiat exchange rate for $primaryFiatCurrency")
        when (val response = exchangeRateRemoteDataSource.fetchExchangeRates("XMR", listOf(primaryFiatCurrency))) {
            is DataResult.Success -> {
                val updatedCache = cachedRates.toMutableMap().apply {
                    putAll(response.data)
                }
                dataStoreLocalDataSource.saveCachedExchangeRates(updatedCache)
                dataStoreLocalDataSource.savePrimaryExchangeRateLastUpdated(now)
                emit(DataResult.Success(response.data))
            }
            is DataResult.Failure -> {
                Log.w(logTag, "Failed to refresh primary fiat rate: ${response.message}")
                if (cachedRate != null) {
                    emit(DataResult.Success(mapOf(primaryFiatCurrency to cachedRate)))
                } else {
                    emit(response)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun fetchReferenceExchangeRates(): Flow<DataResult<ExchangeRateResponse>> = flow {
        val referenceFiatCurrencies = dataStoreLocalDataSource.getReferenceFiatCurrencies().first().distinct()
        if (referenceFiatCurrencies.isEmpty()) {
            emit(DataResult.Success(emptyMap()))
            return@flow
        }

        emit(ensureRatesFor(referenceFiatCurrencies, FULL_REFRESH_INTERVAL_MS))
    }.flowOn(Dispatchers.IO)

    fun fetchExchangeRates(): Flow<DataResult<ExchangeRateResponse>> = flow {
        val fiatCurrencies = dataStoreLocalDataSource.getFiatCurrencies().first().distinct()
        if (fiatCurrencies.isEmpty()) {
            emit(DataResult.Success(emptyMap()))
            return@flow
        }

        emit(ensureRatesFor(fiatCurrencies, FULL_REFRESH_INTERVAL_MS, updatePrimaryTimestamp = true))
    }.flowOn(Dispatchers.IO)

    fun getPrimaryFiatCurrency(): Flow<String> {
        return dataStoreLocalDataSource.getPrimaryFiatCurrency()
    }

    fun getReferenceFiatCurrencies(): Flow<List<String>> {
        return dataStoreLocalDataSource.getReferenceFiatCurrencies()
    }

    private suspend fun ensureRatesFor(
        fiatCurrencies: List<String>,
        refreshIntervalMs: Long,
        updatePrimaryTimestamp: Boolean = false
    ): DataResult<ExchangeRateResponse> {
        val now = System.currentTimeMillis()
        val cachedRates = dataStoreLocalDataSource.getCachedExchangeRates().first()
        val lastFullRefresh = dataStoreLocalDataSource.getExchangeRatesLastUpdated().first()

        val cachedSubset = cachedRates.filterKeys { it in fiatCurrencies }
        val missingCurrencies = fiatCurrencies.filterNot { cachedSubset.containsKey(it) }
        val shouldRefresh = missingCurrencies.isNotEmpty() || isOlderThan(now, lastFullRefresh, refreshIntervalMs)

        if (!shouldRefresh) {
            Log.i(logTag, "Serving cached exchange rates for $fiatCurrencies")
            return DataResult.Success(cachedSubset)
        }

        Log.i(logTag, "Refreshing exchange rates for $fiatCurrencies")
        val response = exchangeRateRemoteDataSource.fetchExchangeRates("XMR", fiatCurrencies)
        if (response is DataResult.Success) {
            val timestamp = now
            val mergedCache = cachedRates.toMutableMap().apply {
                putAll(response.data)
            }
            dataStoreLocalDataSource.saveCachedExchangeRates(mergedCache)
            dataStoreLocalDataSource.saveExchangeRatesLastUpdated(timestamp)
            if (updatePrimaryTimestamp) {
                dataStoreLocalDataSource.savePrimaryExchangeRateLastUpdated(timestamp)
            }
            return DataResult.Success(mergedCache.filterKeys { it in fiatCurrencies })
        } else if (cachedSubset.isNotEmpty()) {
            Log.w(logTag, "Failed to refresh exchange rates, using cached values: ${(response as DataResult.Failure).message}")
            return DataResult.Success(cachedSubset)
        }

        return response
    }

    private fun isOlderThan(now: Long, lastUpdate: Long, interval: Long): Boolean {
        if (lastUpdate == 0L) return true
        return abs(now - lastUpdate) > interval
    }
}
