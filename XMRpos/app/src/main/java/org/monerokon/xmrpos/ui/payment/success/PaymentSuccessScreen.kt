// PaymentSuccessScreen.kt
package org.monerokon.xmrpos.ui.payment.success

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.PaymentSuccess

@Composable
fun PaymentSuccessScreenRoot(viewModel: PaymentSuccessViewModel, navController: NavHostController, fiatAmount: Double, primaryFiatCurrency: String, txId: String, xmrAmount: Double, exchangeRate: Double, timestamp: String, showPrintReceipt: Boolean) {
    viewModel.setNavController(navController)
    PaymentSuccessScreen(
        navigateToEntry = viewModel::navigateToEntry,
        printReceipt = viewModel::printReceipt,
        fiatAmount = fiatAmount,
        primaryFiatCurrency = primaryFiatCurrency,
        txId = txId,
        xmrAmount = xmrAmount,
        exchangeRate = exchangeRate,
        timestamp = timestamp,
        showPrintReceipt = showPrintReceipt,
        printingInProgress = viewModel.printingInProgress
    )
}

@Composable
fun PaymentSuccessScreen(
    navigateToEntry: () -> Unit,
    printReceipt: (PaymentSuccess) -> Unit,
    fiatAmount: Double,
    primaryFiatCurrency: String,
    txId: String,
    xmrAmount: Double,
    exchangeRate: Double,
    timestamp: String,
    showPrintReceipt: Boolean,
    printingInProgress: Boolean
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(innerPadding).padding(horizontal = 48.dp, vertical = 48.dp).fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Column() {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = "Payment successful",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Payment successful",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (showPrintReceipt) {
                FilledTonalButton(
                    onClick = {printReceipt(PaymentSuccess(
                        fiatAmount = fiatAmount,
                        primaryFiatCurrency = primaryFiatCurrency,
                        txId = txId,
                        xmrAmount = xmrAmount,
                        exchangeRate = exchangeRate,
                        timestamp = timestamp,
                        showPrintReceipt = showPrintReceipt
                    ))}
                ) {
                    Text("Print receipt")
                    AnimatedVisibility(
                        visible = printingInProgress,
                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it })
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            ElevatedButton(
                onClick = {navigateToEntry()}
            ) {
                Text("Next order")
            }
        }
    }
}
