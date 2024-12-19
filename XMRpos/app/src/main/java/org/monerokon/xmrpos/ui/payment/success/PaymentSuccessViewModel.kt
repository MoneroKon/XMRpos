// PaymentSuccessViewModel.kt
package org.monerokon.xmrpos.ui.payment.success

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.PrinterRepository
import org.monerokon.xmrpos.ui.PaymentEntry
import org.monerokon.xmrpos.ui.PaymentSuccess
import javax.inject.Inject

@HiltViewModel
class PaymentSuccessViewModel @Inject constructor(
    private val printerRepository: PrinterRepository,
) : ViewModel() {

    init {
        printerRepository.bindPrinterService()
    }

    override fun onCleared() {
        super.onCleared()
        printerRepository.unbindPrinterService()
    }

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToEntry() {
        navController?.navigate(PaymentEntry)
    }

    fun printReceipt(paymentSuccess: PaymentSuccess) {
        viewModelScope.launch {
            printerRepository.printReceipt(paymentSuccess)
        }
    }
}