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
import org.monerokon.xmrpos.data.remote.backend.model.BackendCreateTransactionRequest
import org.monerokon.xmrpos.data.repository.BackendRepository
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.ExchangeRateRepository
import org.monerokon.xmrpos.data.repository.HceRepository
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
    private val backendRepository: BackendRepository,
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

            startPayReceive()
        }
    }

    private fun startPayReceive() {

        viewModelScope.launch(Dispatchers.IO) {
            val backendCreateTransactionRequest = BackendCreateTransactionRequest(
                (targetXMRvalue * 10.0.pow(12)).toLong(),
                "XMRpos",
                paymentValue,
                primaryFiatCurrency,
                dataStoreRepository.getBackendConfValue().first().split("-")[0].toInt()
            )
            /*moneroPayRepository.updateCurrentCallback(callbackUUID, paymentValue);*/
            val response = backendRepository.createTransaction(backendCreateTransactionRequest)

            Log.i(logTag, "MoneroPay: $response")

            if (response is DataResult.Failure) {
                errorMessage = response.message
                stopReceive()
            } else if (response is DataResult.Success) {

                address = response.data.address
                qrCodeUri = "monero:${response.data.address}?tx_amount=${targetXMRvalue}&tx_description=XMRpos"

                backendRepository.observeCurrentTransactionUpdates(response.data.id)

                hceRepository.updateUri(qrCodeUri)
            }
        }
    }

    private fun observePaymentStatus() {
        viewModelScope.launch {
            backendRepository.currentTransactionStatus.collect {
                if (it != null) {
                    if (it.id == backendRepository.currentTransactionId)
                    if (it.accepted) {
                        backendRepository.currentTransactionId = null;
                        navigateToPaymentSuccess(PaymentSuccess(
                            fiatAmount = paymentValue,
                            primaryFiatCurrency = primaryFiatCurrency,
                            txId = it.subTransactions[0].txHash,
                            xmrAmount = it.amount / 10.0.pow(12),
                            exchangeRate = exchangeRates?.get(primaryFiatCurrency) ?: 0.0,
                            timestamp = it.updatedAt,
                            showPrintReceipt = dataStoreRepository.getPrinterConnectionType().first() != "none"
                        ))
                    }
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
        backendRepository.stopObservingTransactionUpdates()
    }

    fun resetErrorMessage() {
        errorMessage = ""
    }

    override fun onCleared() {
        super.onCleared()
        stopReceive()
    }
}