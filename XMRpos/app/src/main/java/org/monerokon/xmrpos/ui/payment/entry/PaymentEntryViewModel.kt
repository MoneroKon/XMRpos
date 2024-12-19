// PaymentEntryViewModel.kt
package org.monerokon.xmrpos.ui.payment.entry

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.ExchangeRateRepository
import org.monerokon.xmrpos.ui.PaymentCheckout
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class PaymentEntryViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {

    private val logTag = "PaymentEntryViewModel"

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    var primaryFiatCurrency by mutableStateOf("");
    var paymentValue by mutableStateOf("0");
    var exchangeRate: Double? by mutableStateOf(null);

    var openSettingsPinCodeDialog by mutableStateOf(false)
    var requirePinCodeOpenSettings by mutableStateOf(true)
    var pinCodeOpenSettings by mutableStateOf("`")

    init {
        fetchExchangeRate()
        // get exchange rate from public api
        viewModelScope.launch {
            dataStoreRepository.getRequirePinCodeOnOpenSettings().collect {  storedRequirePinCodeOpenSettings ->
                requirePinCodeOpenSettings = storedRequirePinCodeOpenSettings
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPinCodeOpenSettings().collect { storedPinCodeOpenSettings ->
                pinCodeOpenSettings = storedPinCodeOpenSettings
            }
        }
    }

    fun fetchExchangeRate() {
        viewModelScope.launch {
            val primaryFiatCurrencyResponse = exchangeRateRepository.getPrimaryFiatCurrency().first()
            primaryFiatCurrency = primaryFiatCurrencyResponse

            val exchangeRateResponse = exchangeRateRepository.fetchPrimaryExchangeRate().first()
            // get value of first key from ExchangeRateResponse (Map<String, Double>)
            exchangeRate = exchangeRateResponse.getOrNull()?.entries?.first()?.value
        }
    }


    fun addDigit(digit: String) {
        fetchExchangeRate()
        // Rules to prevent invalid input
        if (paymentValue.length >= 16) {
            return
        }
        if (paymentValue.isEmpty() && digit == "0") {
            return
        }
        if (paymentValue.isEmpty() && digit == ".") {
            paymentValue += "0."
            return
        }
        if (paymentValue.contains(".") && digit == ".") {
            return
        }
        if (paymentValue == "0" && digit != ".") {
            paymentValue = digit
            return
        }
        paymentValue += digit
    }

    fun removeDigit() {
        if (paymentValue.length == 1) {
            paymentValue = "0"
            return
        }
        if (paymentValue.isNotEmpty()) {
            paymentValue = paymentValue.dropLast(1)
        }
    }

    fun clear() {
        paymentValue = "0"
    }

    fun submit() {
        Log.i(logTag, "Going to next screen!")
        if (paymentValue.toDouble() == 0.0) {
            return
        }
        navController?.navigate(PaymentCheckout(paymentValue.toDouble(), primaryFiatCurrency))
    }

    fun tryOpenSettings() {
        Log.i(logTag, "open settings")
        if (requirePinCodeOpenSettings && pinCodeOpenSettings != "") {
            openSettingsPinCodeDialog = true
        } else {
            navController?.navigate(Settings)
        }
    }

    fun openSettings() {
        navController?.navigate(Settings)
    }

    fun updateOpenSettingsPinCodeDialog(newOpenSettingsPinCodeDialog: Boolean) {
        openSettingsPinCodeDialog = newOpenSettingsPinCodeDialog
    }

}