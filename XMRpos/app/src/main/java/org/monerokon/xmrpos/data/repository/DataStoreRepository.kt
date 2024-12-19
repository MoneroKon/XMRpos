package org.monerokon.xmrpos.data.repository

import kotlinx.coroutines.flow.Flow
import org.monerokon.xmrpos.data.local.datastore.DataStoreLocalDataSource
import javax.inject.Inject

class DataStoreRepository @Inject constructor(
    private val dataStoreLocalDataSource: DataStoreLocalDataSource // Or DataStoreLocalDataSource
) {

    fun getCompanyName(): Flow<String> {
        return dataStoreLocalDataSource.getCompanyName()
    }

    suspend fun saveCompanyName(companyName: String) {
        dataStoreLocalDataSource.saveCompanyName(companyName)
    }

    // Contact Info
    fun getContactInformation(): Flow<String> {
        return dataStoreLocalDataSource.getContactInformation()
    }

    suspend fun saveContactInformation(contactInformation: String) {
        dataStoreLocalDataSource.saveContactInformation(contactInformation)
    }

    fun getReceiptFooter(): Flow<String> {
        return dataStoreLocalDataSource.getReceiptFooter()
    }

    suspend fun saveReceiptFooter(receiptFooter: String) {
        dataStoreLocalDataSource.saveReceiptFooter(receiptFooter)
    }

    // Other DataStore preferences functions
    fun getPrimaryFiatCurrency(): Flow<String> {
        return dataStoreLocalDataSource.getPrimaryFiatCurrency()
    }

    suspend fun savePrimaryFiatCurrency(primaryFiatCurrency: String) {
        dataStoreLocalDataSource.savePrimaryFiatCurrency(primaryFiatCurrency)
    }

    fun getReferenceFiatCurrencies(): Flow<List<String>> {
        return dataStoreLocalDataSource.getReferenceFiatCurrencies()
    }

    suspend fun saveReferenceFiatCurrencies(referenceFiatCurrencies: List<String>) {
        dataStoreLocalDataSource.saveReferenceFiatCurrencies(referenceFiatCurrencies)
    }

    fun getFiatCurrencies(): Flow<List<String>> {
        return dataStoreLocalDataSource.getFiatCurrencies()
    }

    fun getRequirePinCodeOnAppStart(): Flow<Boolean> {
        return dataStoreLocalDataSource.getRequirePinCodeOnAppStart()
    }

    suspend fun saveRequirePinCodeOnAppStart(requirePinCodeOnAppStart: Boolean) {
        dataStoreLocalDataSource.saveRequirePinCodeOnAppStart(requirePinCodeOnAppStart)
    }

    fun getRequirePinCodeOnOpenSettings(): Flow<Boolean> {
        return dataStoreLocalDataSource.getRequirePinCodeOpenSettings()
    }

    suspend fun saveRequirePinCodeOnOpenSettings(requirePinCodeOnOpenSettings: Boolean) {
        dataStoreLocalDataSource.saveRequirePinCodeOpenSettings(requirePinCodeOnOpenSettings)
    }

    fun getPinCodeOnAppStart(): Flow<String> {
        return dataStoreLocalDataSource.getPinCodeOnAppStart()
    }

    suspend fun savePinCodeOnAppStart(pinCodeOnAppStart: String) {
        dataStoreLocalDataSource.savePinCodeOnAppStart(pinCodeOnAppStart)
    }

    fun getPinCodeOpenSettings(): Flow<String> {
        return dataStoreLocalDataSource.getPinCodeOpenSettings()
    }

    suspend fun savePinCodeOpenSettings(pinCodeOnOpenSettings: String) {
        dataStoreLocalDataSource.savePinCodeOpenSettings(pinCodeOnOpenSettings)
    }

    fun getMoneroPayConfValue(): Flow<String> {
        return dataStoreLocalDataSource.getMoneroPayConfValue()
    }

    suspend fun saveMoneroPayConfValue(moneroPayConfValue: String) {
        dataStoreLocalDataSource.saveMoneroPayConfValue(moneroPayConfValue)
    }

    fun getMoneroPayServerAddress(): Flow<String> {
        return dataStoreLocalDataSource.getMoneroPayServerAddress()
    }

    suspend fun saveMoneroPayServerAddress(moneroPayServerAddress: String) {
        dataStoreLocalDataSource.saveMoneroPayServerAddress(moneroPayServerAddress)
    }

    fun getMoneroPayRefreshInterval(): Flow<Int> {
        return dataStoreLocalDataSource.getMoneroPayRefreshInterval()
    }

    suspend fun saveMoneroPayRefreshInterval(moneroPayRefreshInterval: Int) {
        dataStoreLocalDataSource.saveMoneroPayRefreshInterval(moneroPayRefreshInterval)
    }

}
