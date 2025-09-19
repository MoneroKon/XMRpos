package org.monerokon.xmrpos.data.local.datastore

import BACKEND_ACCESS_TOKEN
import BACKEND_CONF_VALUE
import BACKEND_INSTANCE_URL
import BACKEND_REFRESH_INTERVAL
import BACKEND_REFRESH_TOKEN
import COMPANY_NAME
import CONTACT_INFORMATION
import PIN_CODE_ON_APP_START
import PIN_CODE_OPEN_SETTINGS
import PRIMARY_FIAT_CURRENCY
import PRINTER_ADDRESS
import PRINTER_CHARSET_ENCODING
import PRINTER_CHARSET_ID
import PRINTER_CONNECTION_TYPE
import PRINTER_DPI
import PRINTER_NBR_CHARACTERS_PER_LINE
import PRINTER_PORT
import PRINTER_WIDTH
import RECEIPT_FOOTER
import REFERENCE_FIAT_CURRENCIES
import REQUIRE_PIN_CODE_ON_APP_START
import REQUIRE_PIN_CODE_OPEN_SETTINGS
import android.content.Context
import androidx.datastore.preferences.core.edit
import dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreLocalDataSource @Inject constructor(
    private val context: Context
) {
    fun getCompanyName(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[COMPANY_NAME] ?: "My Company"
            }
    }

    suspend fun saveCompanyName(companyName: String) {
        context.dataStore.edit { preferences ->
            preferences[COMPANY_NAME] = companyName
        }
    }

    fun getContactInformation(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[CONTACT_INFORMATION] ?: "123 Main St, Anytown, USA"
            }
    }

    suspend fun saveContactInformation(contactInformation: String) {
        context.dataStore.edit { preferences ->
            preferences[CONTACT_INFORMATION] = contactInformation
        }
    }

    fun getReceiptFooter(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[RECEIPT_FOOTER] ?: "Thank you for your business!"
            }
    }

    suspend fun saveReceiptFooter(receiptFooter: String) {
        context.dataStore.edit { preferences ->
            preferences[RECEIPT_FOOTER] = receiptFooter
        }
    }

    fun getPrimaryFiatCurrency(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRIMARY_FIAT_CURRENCY] ?: "USD"
            }
    }

    suspend fun savePrimaryFiatCurrency(primaryFiatCurrency: String) {
        context.dataStore.edit { preferences ->
            preferences[PRIMARY_FIAT_CURRENCY] = primaryFiatCurrency
        }
    }

    fun getReferenceFiatCurrencies(): Flow<List<String>> {
        return context.dataStore.data
            .map { preferences ->
                val joinedString: String? = preferences[REFERENCE_FIAT_CURRENCIES]
                if (joinedString != null && joinedString.toString() != "") joinedString.split(",") else if (joinedString.toString() == "") emptyList() else listOf("EUR", "CZK", "MXN")
            }
    }

    suspend fun saveReferenceFiatCurrencies(referenceFiatCurrencies: List<String>) {
        val joinedString = referenceFiatCurrencies.joinToString(",")
        context.dataStore.edit { preferences ->
            preferences[REFERENCE_FIAT_CURRENCIES] = joinedString
        }
    }

    fun getFiatCurrencies(): Flow<List<String>> {
        val primaryFiatCurrency = getPrimaryFiatCurrency()
        val referenceFiatCurrencies = getReferenceFiatCurrencies()
        val joinedList = mutableListOf<String>()
        return primaryFiatCurrency.combine(referenceFiatCurrencies) { primary, reference ->
            joinedList.add(primary)
            joinedList.addAll(reference)
            joinedList
        }
    }

    fun getRequirePinCodeOnAppStart(): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences ->
                preferences[REQUIRE_PIN_CODE_ON_APP_START] ?: false
            }
    }

    suspend fun saveRequirePinCodeOnAppStart(requirePinCodeOnAppStart: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REQUIRE_PIN_CODE_ON_APP_START] = requirePinCodeOnAppStart
        }
    }

    fun getRequirePinCodeOpenSettings(): Flow<Boolean> {
        return context.dataStore.data
            .map { preferences ->
                preferences[REQUIRE_PIN_CODE_OPEN_SETTINGS] ?: false
            }
    }

    suspend fun saveRequirePinCodeOpenSettings(requirePinCodeOpenSettings: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REQUIRE_PIN_CODE_OPEN_SETTINGS] = requirePinCodeOpenSettings
        }
    }

    fun getPinCodeOnAppStart(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PIN_CODE_ON_APP_START] ?: ""
            }
    }

    suspend fun savePinCodeOnAppStart(pinCodeOnAppStart: String) {
        context.dataStore.edit { preferences ->
            preferences[PIN_CODE_ON_APP_START] = pinCodeOnAppStart
        }
    }

    fun getPinCodeOpenSettings(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PIN_CODE_OPEN_SETTINGS] ?: ""
            }
    }

    suspend fun savePinCodeOpenSettings(pinCodeOpenSettings: String) {
        context.dataStore.edit { preferences ->
            preferences[PIN_CODE_OPEN_SETTINGS] = pinCodeOpenSettings
        }
    }

    fun getBackendConfValue(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BACKEND_CONF_VALUE] ?: "0-conf"
            }
    }

    suspend fun saveBackendConfValue(backendConfValue: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKEND_CONF_VALUE] = backendConfValue
        }
    }

    fun getBackendInstanceUrl(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BACKEND_INSTANCE_URL] ?: "http://192.168.1.100:5000"
            }
    }

    suspend fun saveBackendInstanceUrl(backendInstanceUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKEND_INSTANCE_URL] = backendInstanceUrl
        }
    }

    fun getBackendAccessToken(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BACKEND_ACCESS_TOKEN] ?: ""
            }
    }

    suspend fun saveBackendAccessToken(backendAccessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKEND_ACCESS_TOKEN] = backendAccessToken
        }
    }

    fun getBackendRefreshToken(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BACKEND_REFRESH_TOKEN] ?: ""
            }
    }

    suspend fun saveBackendRefreshToken(backendRefreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKEND_REFRESH_TOKEN] = backendRefreshToken
        }
    }

    fun getBackendRequestInterval(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BACKEND_REFRESH_INTERVAL] ?: 5
            }
    }

    suspend fun saveBackendRequestInterval(backendRequestInterval: Int) {
        context.dataStore.edit { preferences ->
            preferences[BACKEND_REFRESH_INTERVAL] = backendRequestInterval
        }
    }

    fun getPrinterConnectionType(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_CONNECTION_TYPE] ?: "none"
            }
    }

    suspend fun savePrinterConnectionType(printerConnectionType: String) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_CONNECTION_TYPE] = printerConnectionType
        }
    }

    fun getPrinterDpi(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_DPI] ?: 203
            }
    }

    suspend fun savePrinterDpi(printerDpi: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_DPI] = printerDpi
        }
    }

    fun getPrinterWidth(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_WIDTH] ?: 48
            }
    }

    suspend fun savePrinterWidth(printerWidth: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_WIDTH] = printerWidth
        }
    }

    fun getPrinterNbrCharactersPerLine(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_NBR_CHARACTERS_PER_LINE] ?: 30
            }
    }

    suspend fun savePrinterNbrCharactersPerLine(printerNbrCharactersPerLine: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_NBR_CHARACTERS_PER_LINE] = printerNbrCharactersPerLine
        }
    }

    fun getPrinterCharsetEncoding(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_CHARSET_ENCODING] ?: "UTF-8"
            }
    }

    suspend fun savePrinterCharsetEncoding(printerCharsetEncoding: String) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_CHARSET_ENCODING] = printerCharsetEncoding
        }
    }

    fun getPrinterCharsetId(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_CHARSET_ID] ?: 16
            }
    }

    suspend fun savePrinterCharsetId(printerCharsetId: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_CHARSET_ID] = printerCharsetId
        }
    }

    fun getPrinterAddress(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_ADDRESS] ?: ""
            }
    }

    suspend fun savePrinterAddress(printerAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_ADDRESS] = printerAddress
        }
    }

    fun getPrinterPort(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[PRINTER_PORT] ?: 9100
            }
    }

    suspend fun savePrinterPort(printerPort: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_PORT] = printerPort
        }
    }

    suspend fun clearDataStore() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

}