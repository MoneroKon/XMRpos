package org.monerokon.xmrpos.data.local.datastore

import COMPANY_NAME
import CONTACT_INFORMATION
import MONERO_PAY_CONF_VALUE
import MONERO_PAY_REFRESH_INTERVAL
import MONERO_PAY_SERVER_ADDRESS
import PIN_CODE_ON_APP_START
import PIN_CODE_OPEN_SETTINGS
import PRIMARY_FIAT_CURRENCY
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

    fun getMoneroPayConfValue(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[MONERO_PAY_CONF_VALUE] ?: "0-conf"
            }
    }

    suspend fun saveMoneroPayConfValue(moneroPayConfValue: String) {
        context.dataStore.edit { preferences ->
            preferences[MONERO_PAY_CONF_VALUE] = moneroPayConfValue
        }
    }

    fun getMoneroPayServerAddress(): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[MONERO_PAY_SERVER_ADDRESS] ?: "http://192.168.1.100:5000"
            }
    }

    suspend fun saveMoneroPayServerAddress(moneroPayServerAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[MONERO_PAY_SERVER_ADDRESS] = moneroPayServerAddress
        }
    }

    fun getMoneroPayRefreshInterval(): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[MONERO_PAY_REFRESH_INTERVAL] ?: 5
            }
    }

    suspend fun saveMoneroPayRefreshInterval(moneroPayRefreshInterval: Int) {
        context.dataStore.edit { preferences ->
            preferences[MONERO_PAY_REFRESH_INTERVAL] = moneroPayRefreshInterval
        }
    }

}