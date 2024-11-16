// PaymentCheckoutViewModel.kt
package org.monerokon.xmrpos.ui.payment

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.data.ExchangeRateManager
import org.monerokon.xmrpos.data.MoneroPayCallbackServer
import org.monerokon.xmrpos.data.MoneroPayManager
import org.monerokon.xmrpos.ui.PaymentEntry
import org.monerokon.xmrpos.ui.PaymentSuccess
import java.net.NetworkInterface
import java.util.Hashtable
import kotlin.collections.set

class PaymentCheckoutViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    var paymentValue by mutableDoubleStateOf(0.0)
    var primaryFiatCurrency by mutableStateOf("")
    var referenceFiatCurrencies by mutableStateOf(listOf<String>())
    var exchangeRates: Map<String, Double>? by mutableStateOf(null)
    var targetXMRvalue by mutableDoubleStateOf(0.0)

    var moneroPayServerAddress by mutableStateOf("")
    var qrCodeUri by mutableStateOf("")
    var address by mutableStateOf("")

    init {
        // get exchange rate from public api
        fetchReferenceFiatCurrencies()
    }

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateBack() {
        navController?.navigate(PaymentEntry)
    }

    fun updatePaymentValue(value: Double) {
        paymentValue = value
    }

    fun updatePrimaryFiatCurrency(value: String) {
        primaryFiatCurrency = value
    }

    fun fetchReferenceFiatCurrencies() {
        CoroutineScope(Dispatchers.IO).launch {
            val newReferenceFiatCurrencies = dataStoreManager.getStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES).first()
            if (newReferenceFiatCurrencies != null) {
                withContext(Dispatchers.Main) {
                    referenceFiatCurrencies = newReferenceFiatCurrencies
                    println("referenceFiatCurrencies: $newReferenceFiatCurrencies")
                    val newReferenceFiatCurrenciesWithPrimary = newReferenceFiatCurrencies + primaryFiatCurrency
                    fetchExchangeRates(newReferenceFiatCurrenciesWithPrimary)
                }
            }
        }
    }

    fun fetchExchangeRates(currencies: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val rates = ExchangeRateManager.fetchExchangeRates("XMR", currencies)
            rates?.let {
                withContext(Dispatchers.Main) {
                    println("rates: $it")
                    exchangeRates = it
                    calculateTargetXMRvalue()
                }
            }
        }
    }

    fun calculateTargetXMRvalue() {
        exchangeRates?.get(primaryFiatCurrency).let {
            if (it != null) {
                targetXMRvalue = paymentValue / it
                startReceive()
            }
        }
    }

    fun startReceive() {
        CoroutineScope(Dispatchers.IO).launch {
            val newMoneroPayServerAddress = dataStoreManager.getString(DataStoreManager.MONERO_PAY_SERVER_ADDRESS).first()
            if (newMoneroPayServerAddress != null) {
                withContext(Dispatchers.Main) {
                    moneroPayServerAddress = newMoneroPayServerAddress
                    println("moneroPayServerAddress: $newMoneroPayServerAddress")
                    val ipAddress = getDeviceIpAddress()
                        if (ipAddress != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val response = MoneroPayManager(moneroPayServerAddress).startReceive((targetXMRvalue * 1000000000000).toLong(), "XMRPOS", "http://$ipAddress:8080")
                                withContext(Dispatchers.Main) {
                                    if (response != null) {
                                        println("DID IT: $response")
                                        address = response.address
                                        qrCodeUri = "monero:${response.address}?tx_amount=${response.amount}&tx_description=${response.description}"
                                        startReceiveStatusCheck()
                                    } else {
                                        print("DID NOT DO IT")
                                    }
                                }
                            }
                        } else {
                            println("Failed to get IP address")
                        }
                    }
                }
            }
        }

    fun navigateToPaymentSuccess(paymentSuccess: PaymentSuccess) {
        navController?.navigate(paymentSuccess)
    }

    fun startReceiveStatusCheck() {
        MoneroPayCallbackServer(8080) { paymentCallback ->
            if (paymentCallback.amount.expected == paymentCallback.amount.covered.total) {
                println("Payment received!")
                navigateToPaymentSuccess(PaymentSuccess(11.5, "USD", 0.33))
            }
        }.start()
    }

    fun getDeviceIpAddress(): String? {
        return NetworkInterface
            .getNetworkInterfaces()
            .toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it.isSiteLocalAddress && it.hostAddress.startsWith("192.") }
            ?.hostAddress

    }

    fun generateQRCode(text: String, width: Int, height: Int, margin: Int, color: Int, background: Int): Bitmap {
        val writer = QRCodeWriter()
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.MARGIN] = margin
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) color else background)
            }
        }
        return bmp
    }
}