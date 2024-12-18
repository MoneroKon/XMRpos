package org.monerokon.xmrpos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.monerokon.xmrpos.data.local.datastore.DataStoreLocalDataSource
import org.monerokon.xmrpos.data.remote.exchangeRate.ExchangeRateApi
import org.monerokon.xmrpos.data.remote.exchangeRate.ExchangeRateRemoteDataSource
import org.monerokon.xmrpos.data.repository.ExchangeRateRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExchangeRateModule {

    @Provides
    @Named("exchangeRateRetrofit")
    fun provideExchangeRateRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://min-api.cryptocompare.com/data/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    fun provideExchangeRateApi(@Named("exchangeRateRetrofit") exchangeRateRetrofit: Retrofit): ExchangeRateApi {
        return exchangeRateRetrofit.create(ExchangeRateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExchangeRateRepository(
        exchangeRateRemoteDataSource: ExchangeRateRemoteDataSource,
        DataStoreLocalDataSource: DataStoreLocalDataSource
    ): ExchangeRateRepository {
        return ExchangeRateRepository(exchangeRateRemoteDataSource, DataStoreLocalDataSource)
    }
}