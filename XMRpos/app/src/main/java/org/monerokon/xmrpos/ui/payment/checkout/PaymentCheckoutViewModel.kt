// PaymentCheckoutViewModel.kt
package org.monerokon.xmrpos.ui.payment.checkout

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.ExchangeRateRepository
import org.monerokon.xmrpos.data.repository.HceRepository
import org.monerokon.xmrpos.data.repository.MoneroPayRepository
import org.monerokon.xmrpos.shared.DataResult
import org.monerokon.xmrpos.ui.PaymentEntry
import org.monerokon.xmrpos.ui.PaymentSuccess
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.NetworkInterface
import java.util.Hashtable
import java.util.UUID
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class PaymentCheckoutViewModel @Inject constructor(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val moneroPayRepository: MoneroPayRepository,
    private val hceRepository: HceRepository,
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {

    private val logTag = "PaymentCheckoutViewModel"

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateBack() {
        stopReceive()
        navController?.navigate(PaymentEntry)
    }

    var paymentValue by mutableStateOf(0.0)
    var primaryFiatCurrency by mutableStateOf("")
    var referenceFiatCurrencies by mutableStateOf(listOf<String>())
    var exchangeRates: Map<String, Double>? by mutableStateOf(null)
    var targetXMRvalue by mutableStateOf(0.0)

    var moneroPayServerAddress by mutableStateOf("")
    var qrCodeUri by mutableStateOf("")
    var address by mutableStateOf("")
    var errorMessage by mutableStateOf("")

    init {
        fetchExchangeRates()
        observePaymentStatus()
    }

    fun updatePaymentValue(value: Double) {
        paymentValue = value
    }

    fun updatePrimaryFiatCurrency(value: String) {
        primaryFiatCurrency = value
    }

    private fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            val primaryFiatCurrencyResponse = exchangeRateRepository.getPrimaryFiatCurrency().first()
            primaryFiatCurrency = primaryFiatCurrencyResponse

            val referenceFiatCurrenciesResponse = exchangeRateRepository.getReferenceFiatCurrencies().first()
            referenceFiatCurrencies = referenceFiatCurrenciesResponse

            val exchangeRatesResponse = exchangeRateRepository.fetchExchangeRates().first()

            if (exchangeRatesResponse is DataResult.Failure) {
                errorMessage = exchangeRatesResponse.message
            } else if (exchangeRatesResponse is DataResult.Success) {
                exchangeRates = exchangeRatesResponse.data
            }

            targetXMRvalue = BigDecimal(paymentValue / (exchangeRates?.get(primaryFiatCurrency) ?: 0.0)).setScale(12, RoundingMode.UP).toDouble()

            Log.i(logTag, "Reference exchange rates: $referenceFiatCurrencies")
            Log.i(logTag, "Exchange rates: $exchangeRates")

            startMoneroPayReceive()
        }
    }

    private fun startMoneroPayReceive() {
        val ipAddress = getDeviceIpAddress()
        val callbackUUID = UUID.randomUUID().toString()
        val moneroPayReceiveRequest = MoneroPayReceiveRequest(
            (targetXMRvalue * 10.0.pow(12)).toLong(), "XMRPOS", "http://$ipAddress:8080?fiatValue=$paymentValue&callbackUUID=$callbackUUID"
        )
        viewModelScope.launch(Dispatchers.IO) {
            moneroPayRepository.updateCurrentCallback(callbackUUID, paymentValue);
            val response = moneroPayRepository.startReceive(moneroPayReceiveRequest)

            Log.i(logTag, "MoneroPay: $response")

            if (response is DataResult.Failure) {
                errorMessage = response.message
                moneroPayRepository.updateCurrentCallback(null, null)
            } else if (response is DataResult.Success) {

                address = response.data.address
                qrCodeUri = "monero:${response.data.address}?tx_amount=${targetXMRvalue}&tx_description=${response.data.description}"

                hceRepository.updateUri(qrCodeUri)
            }
        }
    }

    private fun getDeviceIpAddress(): String? {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }

    private fun observePaymentStatus() {
        viewModelScope.launch {
            moneroPayRepository.paymentStatus.collect { paymentCallback ->
                paymentCallback?.let {
                    stopReceive()
                    navigateToPaymentSuccess(PaymentSuccess(
                        fiatAmount = paymentValue,
                        primaryFiatCurrency = primaryFiatCurrency,
                        txId = it.transaction.tx_hash,
                        xmrAmount = it.amount.covered.total / 10.0.pow(12),
                        exchangeRate = exchangeRates?.get(primaryFiatCurrency) ?: 0.0,
                        timestamp = it.transaction.timestamp,
                        showPrintReceipt = dataStoreRepository.getPrinterConnectionType().first() != "none"
                    ))
                }
            }
        }
    }

    fun navigateToPaymentSuccess(paymentSuccess: PaymentSuccess) {
        navController?.navigate(paymentSuccess)
    }

    fun generateQRCode(text: String, width: Int, height: Int, margin: Int, color: Int, background: Int): Bitmap {
        val writer = QRCodeWriter()
        val hints = Hashtable<EncodeHintType, Any>().apply {
            this[EncodeHintType.MARGIN] = margin
        }
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setPixel(x, y, if (bitMatrix[x, y]) color else background)
                }
            }
        }
    }

    fun stopReceive() {
        hceRepository.updateUri("")
        moneroPayRepository.stopReceive()
    }

    fun resetErrorMessage() {
        errorMessage = ""
    }

    override fun onCleared() {
        super.onCleared()
        stopReceive()
    }
}