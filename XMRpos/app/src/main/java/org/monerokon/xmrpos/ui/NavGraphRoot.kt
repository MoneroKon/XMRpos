// NavGraph.kt
package org.monerokon.xmrpos.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.monerokon.xmrpos.ui.payment.checkout.PaymentCheckoutScreenRoot
import org.monerokon.xmrpos.ui.payment.checkout.PaymentCheckoutViewModel
import org.monerokon.xmrpos.ui.payment.entry.PaymentEntryScreenRoot
import org.monerokon.xmrpos.ui.payment.entry.PaymentEntryViewModel
import org.monerokon.xmrpos.ui.payment.login.LoginScreenRoot
import org.monerokon.xmrpos.ui.payment.login.LoginViewModel
import org.monerokon.xmrpos.ui.payment.success.PaymentSuccessScreenRoot
import org.monerokon.xmrpos.ui.payment.success.PaymentSuccessViewModel
import org.monerokon.xmrpos.ui.settings.backend.BackendScreenRoot
import org.monerokon.xmrpos.ui.settings.backend.BackendViewModel
import org.monerokon.xmrpos.ui.settings.companyinformation.CompanyInformationScreenRoot
import org.monerokon.xmrpos.ui.settings.companyinformation.CompanyInformationViewModel
import org.monerokon.xmrpos.ui.settings.exporttransactions.ExportTransactionsScreenRoot
import org.monerokon.xmrpos.ui.settings.exporttransactions.ExportTransactionsViewModel
import org.monerokon.xmrpos.ui.settings.fiatcurrencies.FiatCurrenciesScreenRoot
import org.monerokon.xmrpos.ui.settings.fiatcurrencies.FiatCurrenciesViewModel
import org.monerokon.xmrpos.ui.settings.main.MainSettingsScreenRoot
import org.monerokon.xmrpos.ui.settings.main.MainSettingsViewModel
import org.monerokon.xmrpos.ui.settings.moneropay.SecurityScreenRoot
import org.monerokon.xmrpos.ui.settings.moneropay.SecurityViewModel
import org.monerokon.xmrpos.ui.settings.printersettings.PrinterSettingsScreenRoot
import org.monerokon.xmrpos.ui.settings.printersettings.PrinterSettingsViewModel

@Composable
fun NavGraphRoot(
    navController: NavHostController = rememberNavController(),
    startDestination: Any,
) {
    Scaffold (modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = {
                    slideIn(initialOffset = { fullSize -> IntOffset(fullSize.width, 0) }, animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    slideOut(targetOffset = { fullSize -> IntOffset(fullSize.width, 0) }, animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<Login> {
                    val loginViewModel: LoginViewModel = hiltViewModel()
                    LoginScreenRoot(viewModel = loginViewModel, navController = navController)
                }
                composable<PaymentEntry> {
                    val paymentEntryViewModel: PaymentEntryViewModel =  hiltViewModel()
                    PaymentEntryScreenRoot(
                        viewModel = paymentEntryViewModel,
                        navController = navController
                    )
                }
                composable<PaymentCheckout> {
                    val args = it.toRoute<PaymentCheckout>()
                    val paymentCheckoutViewModel: PaymentCheckoutViewModel = hiltViewModel()
                    PaymentCheckoutScreenRoot(viewModel = paymentCheckoutViewModel, navController = navController, fiatAmount = args.fiatAmount, primaryFiatCurrency = args.primaryFiatCurrency)
                }
                composable<PaymentSuccess> {
                    val args = it.toRoute<PaymentSuccess>()
                    val paymentSuccessViewModel: PaymentSuccessViewModel = hiltViewModel()
                    PaymentSuccessScreenRoot(viewModel = paymentSuccessViewModel, navController = navController, fiatAmount = args.fiatAmount, primaryFiatCurrency = args.primaryFiatCurrency, txId = args.txId, xmrAmount = args.xmrAmount, exchangeRate = args.exchangeRate, timestamp = args.timestamp, showPrintReceipt = args.showPrintReceipt)
                }
                composable<Settings> {
                    val mainSettingsViewModel: MainSettingsViewModel = viewModel()
                    MainSettingsScreenRoot(viewModel = mainSettingsViewModel, navController = navController)
                }
                composable<CompanyInformation> {
                    val companyInformationViewModel: CompanyInformationViewModel = hiltViewModel()
                    CompanyInformationScreenRoot(viewModel = companyInformationViewModel, navController = navController)
                }
                composable<FiatCurrencies> {
                    val fiatCurrenciesViewModel: FiatCurrenciesViewModel = hiltViewModel()
                    FiatCurrenciesScreenRoot(viewModel = fiatCurrenciesViewModel, navController = navController)
                }
                composable<Security> {
                    val securityViewModel: SecurityViewModel = hiltViewModel()
                    SecurityScreenRoot(viewModel = securityViewModel, navController = navController)
                }
                composable<ExportTransactions> {
                    val exportTransactionsViewModel: ExportTransactionsViewModel = hiltViewModel()
                    ExportTransactionsScreenRoot(viewModel = exportTransactionsViewModel, navController = navController)
                }
                composable<Backend> {
                    val backendViewModel: BackendViewModel = hiltViewModel()
                    BackendScreenRoot(viewModel = backendViewModel, navController = navController)
                }
                composable<PrinterSettings> {
                    val printerSettingsViewModel: PrinterSettingsViewModel = hiltViewModel()
                    PrinterSettingsScreenRoot(viewModel = printerSettingsViewModel, navController = navController)
                }
            }
        }

    }
}

@Serializable
object Login

@Serializable
object PaymentEntry

@Serializable
data class PaymentCheckout(
    val fiatAmount: Double,
    val primaryFiatCurrency: String,
)

@Serializable
data class PaymentSuccess(
    val fiatAmount: Double,
    val primaryFiatCurrency: String,
    val txId: String,
    val xmrAmount: Double,
    val exchangeRate: Double,
    val timestamp: String,
    val showPrintReceipt: Boolean,
)

// Settings routes

@Serializable
object Settings

@Serializable
object CompanyInformation

@Serializable
object FiatCurrencies

@Serializable
object Security

@Serializable
object ExportTransactions

@Serializable
object Backend

@Serializable
object PrinterSettings
