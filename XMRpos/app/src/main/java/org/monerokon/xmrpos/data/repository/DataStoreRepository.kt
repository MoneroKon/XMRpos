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

    fun getBackendConfValue(): Flow<String> {
        return dataStoreLocalDataSource.getBackendConfValue()
    }

    suspend fun saveBackendConfValue(backendConfValue: String) {
        dataStoreLocalDataSource.saveBackendConfValue(backendConfValue)
    }

    fun getBackendInstanceUrl(): Flow<String> {
        return dataStoreLocalDataSource.getBackendInstanceUrl()
    }

    suspend fun saveBackendInstanceUrl(backendInstanceUrl: String) {
        dataStoreLocalDataSource.saveBackendInstanceUrl(backendInstanceUrl)
    }

    fun getBackendAccessToken(): Flow<String> {
        return dataStoreLocalDataSource.getBackendAccessToken()
    }

    suspend fun saveBackendAccessToken(backendAccessToken: String) {
        dataStoreLocalDataSource.saveBackendAccessToken(backendAccessToken)
    }

    fun getBackendRefreshToken(): Flow<String> {
        return dataStoreLocalDataSource.getBackendRefreshToken()
    }

    suspend fun saveBackendRefreshToken(backendRefreshToken: String) {
        dataStoreLocalDataSource.saveBackendRefreshToken(backendRefreshToken)
    }

    fun getBackendRequestInterval(): Flow<Int> {
        return dataStoreLocalDataSource.getBackendRequestInterval()
    }

    suspend fun saveBackendRequestInterval(backendRequestInterval: Int) {
        dataStoreLocalDataSource.saveBackendRequestInterval(backendRequestInterval)
    }

    fun getPrinterConnectionType(): Flow<String> {
        return dataStoreLocalDataSource.getPrinterConnectionType()
    }

    suspend fun savePrinterConnectionType(printerConnectionType: String) {
        dataStoreLocalDataSource.savePrinterConnectionType(printerConnectionType)
    }

    fun getPrinterDpi(): Flow<Int> {
        return dataStoreLocalDataSource.getPrinterDpi()
    }

    suspend fun savePrinterDpi(printerDpi: Int) {
        dataStoreLocalDataSource.savePrinterDpi(printerDpi)
    }

    fun getPrinterWidth(): Flow<Int> {
        return dataStoreLocalDataSource.getPrinterWidth()
    }

    suspend fun savePrinterWidth(printerWidth: Int) {
        dataStoreLocalDataSource.savePrinterWidth(printerWidth)
    }

    fun getPrinterNbrCharactersPerLine(): Flow<Int> {
        return dataStoreLocalDataSource.getPrinterNbrCharactersPerLine()
    }

    suspend fun savePrinterNbrCharactersPerLine(printerNbrCharactersPerLine: Int) {
        dataStoreLocalDataSource.savePrinterNbrCharactersPerLine(printerNbrCharactersPerLine)
    }

    fun getPrinterCharsetEncoding(): Flow<String> {
        return dataStoreLocalDataSource.getPrinterCharsetEncoding()
    }

    suspend fun savePrinterCharsetEncoding(printerCharsetEncoding: String) {
        dataStoreLocalDataSource.savePrinterCharsetEncoding(printerCharsetEncoding)
    }

    fun getPrinterCharsetId(): Flow<Int> {
        return dataStoreLocalDataSource.getPrinterCharsetId()
    }

    suspend fun savePrinterCharsetId(printerCharsetId: Int) {
        dataStoreLocalDataSource.savePrinterCharsetId(printerCharsetId)
    }

    fun getPrinterAddress(): Flow<String> {
        return dataStoreLocalDataSource.getPrinterAddress()
    }

    suspend fun savePrinterAddress(printerAddress: String) {
        dataStoreLocalDataSource.savePrinterAddress(printerAddress)
    }

    fun getPrinterPort(): Flow<Int> {
        return dataStoreLocalDataSource.getPrinterPort()
    }

    suspend fun savePrinterPort(printerPort: Int) {
        dataStoreLocalDataSource.savePrinterPort(printerPort)
    }

    suspend fun clearDataStore() {
        dataStoreLocalDataSource.clearDataStore()
    }

}
