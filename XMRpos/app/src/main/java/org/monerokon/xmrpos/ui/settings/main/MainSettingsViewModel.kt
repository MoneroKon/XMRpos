// MainSettingsViewModel.kt
package org.monerokon.xmrpos.ui.settings.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.Backend
import org.monerokon.xmrpos.ui.CompanyInformation
import org.monerokon.xmrpos.ui.ExportTransactions
import org.monerokon.xmrpos.ui.FiatCurrencies
import org.monerokon.xmrpos.ui.PaymentEntry
import org.monerokon.xmrpos.ui.PrinterSettings
import org.monerokon.xmrpos.ui.Security

class MainSettingsViewModel (private val savedStateHandle: SavedStateHandle): ViewModel() {

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToPayment() {
        navController?.navigate(PaymentEntry)
    }

    fun navigateToCompanyInformation() {
        navController?.navigate(CompanyInformation)
    }

    fun navigateToFiatCurrencies() {
        navController?.navigate(FiatCurrencies)
    }

    fun navigateToSecurity() {
        navController?.navigate(Security)
    }

    fun navigateToExportTransactions() {
        navController?.navigate(ExportTransactions)
    }

    fun navigateToBackend() {
        navController?.navigate(Backend)
    }

    fun navigateToPrinterSettings() {
        navController?.navigate(PrinterSettings)
    }
}


