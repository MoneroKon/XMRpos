package org.monerokon.xmrpos.ui.payment.checkout

import CurrencyConverterCard
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.R
import org.monerokon.xmrpos.ui.PaymentSuccess
import org.monerokon.xmrpos.ui.common.composables.CustomAlertDialog

@Composable
fun PaymentCheckoutScreenRoot(viewModel: PaymentCheckoutViewModel, navController: NavHostController, fiatAmount: Double, primaryFiatCurrency: String) {
    viewModel.setNavController(navController)
    viewModel.updatePaymentValue(fiatAmount)
    viewModel.updatePrimaryFiatCurrency(primaryFiatCurrency)
    PaymentCheckoutScreen(
        paymentValue = fiatAmount,
        primaryFiatCurrency = primaryFiatCurrency,
        referenceFiatCurrencies = viewModel.referenceFiatCurrencies,
        exchangeRates = viewModel.exchangeRates,
        targetXMRvalue = viewModel.targetXMRvalue,
        qrCodeUri = viewModel.qrCodeUri,
        generateQRCode = viewModel::generateQRCode,
        navigateBack = viewModel::navigateBack,
        errorMessage = viewModel.errorMessage,
        resetErrorMessage = viewModel::resetErrorMessage,
        navigateToPaymentSuccess = viewModel::navigateToPaymentSuccess
    )
}

@Composable
fun PaymentCheckoutScreen(
    paymentValue: Double,
    primaryFiatCurrency: String,
    referenceFiatCurrencies: List<String>,
    exchangeRates: Map<String, Double>?,
    targetXMRvalue: Double,
    qrCodeUri: String,
    generateQRCode: (String, Int, Int, Int, Int, Int) -> Bitmap,
    navigateBack: () -> Unit,
    errorMessage: String,
    resetErrorMessage: () -> Unit,
    navigateToPaymentSuccess: (PaymentSuccess) -> Unit
) {
    val openAlertDialog = remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(innerPadding).padding(horizontal = 48.dp).fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            QRCodeWithImage(qrCodeUri, generateQRCode)
            Spacer(modifier = Modifier.height(32.dp))
            CurrencyConverterCard(primaryFiatCurrency, exchangeRates?.get(primaryFiatCurrency), paymentValue.toString(), targetXMRvalue = targetXMRvalue)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedCard (
                modifier = Modifier.fillMaxWidth().height(200.dp),
            ) {
                LazyColumn(

                ) {
                    items(referenceFiatCurrencies.size) { index ->
                        CurrencyConverterCard(referenceFiatCurrencies[index], exchangeRates?.get(referenceFiatCurrencies[index]), paymentValue.toString(), targetXMRvalue = targetXMRvalue, elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), color = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))
                        if (index < referenceFiatCurrencies.size - 1) arrayOf(
                            HorizontalDivider()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text("Waiting on payment to complete", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(onClick = {openAlertDialog.value = !openAlertDialog.value}) {
                    Text("Back")
                }
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator()
            }
            when {
                errorMessage != "" -> {
                    CustomAlertDialog(
                        onDismissRequest = { resetErrorMessage()},
                        onConfirmation = {
                            resetErrorMessage()
                        },
                        dialogTitle = "Error",
                        dialogText = errorMessage,
                        confirmButtonText = "Ok",
                        dismissButtonText = null,
                        icon = Icons.Default.Warning
                    )
                }
            }
            when {
                openAlertDialog.value -> {
                    CustomAlertDialog(
                        onDismissRequest = { openAlertDialog.value = false },
                        onConfirmation = {
                            openAlertDialog.value = false
                            navigateBack()
                        },
                        dialogTitle = "Warning",
                        dialogText = "If you go back while the customer is paying, the payment will not be confirmed in the app. Please only go back if the customer has not started paying yet.",
                        confirmButtonText = "Go back",
                        dismissButtonText = "Stay here",
                        icon = Icons.Default.Warning
                    )
                }
            }
        }
    }
}

// QR code with image in center
@Composable
fun QRCodeWithImage(uri: String, generateQRCode: (String, Int, Int, Int, Int, Int) -> Bitmap) {
    var qrCodeBitmap: Bitmap? = null
    if (uri != "") {
        qrCodeBitmap = generateQRCode(uri, 400, 400, 1, 0xFF000000.toInt(), MaterialTheme.colorScheme.primary.hashCode())
    }
    Box (
        contentAlignment = Alignment.Center
    ) {
        if (qrCodeBitmap != null) {
            Image(
                bitmap = qrCodeBitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(280.dp).clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier.size(280.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
            )
        }
        Image(
            painterResource(R.drawable.monero_symbol),
            contentDescription = null,
            modifier = Modifier.requiredSize(80.dp),
        )

    }
}