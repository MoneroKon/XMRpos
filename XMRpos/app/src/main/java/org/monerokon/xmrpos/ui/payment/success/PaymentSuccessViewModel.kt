package org.monerokon.xmrpos.ui.payment.success

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToEntry() {
        navController?.navigate(PaymentEntry)
    }

    var printingInProgress by mutableStateOf(false)

    fun printReceipt(paymentSuccess: PaymentSuccess) {
        printingInProgress = true
        viewModelScope.launch {
            printerRepository.printReceipt(paymentSuccess)
            printingInProgress = false
        }
    }
}