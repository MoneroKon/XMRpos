// PaymentEntryScreen.kt
package org.monerokon.xmrpos.ui.payment.entry

import CurrencyConverterCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

// PaymentEntryScreenRoot
@Composable
fun PaymentEntryScreenRoot(viewModel: PaymentEntryViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    PaymentEntryScreen(
        paymentValue = viewModel.paymentValue,
        primaryFiatCurrency = viewModel.primaryFiatCurrency,
        exchangeRate = viewModel.exchangeRate,
        onDigitClick = viewModel::addDigit,
        onBackspaceClick = viewModel::removeDigit,
        onClearClick = viewModel::clear,
        onSubmitClick = viewModel::submit,
        onSettingsClick = viewModel::tryOpenSettings,
        openSettingsPinCodeDialog = viewModel.openSettingsPinCodeDialog,
        pinCodeOpenSettings = viewModel.pinCodeOpenSettings,
        updateOpenSettingsPinCodeDialog = viewModel::updateOpenSettingsPinCodeDialog,
        openSettings = viewModel::openSettings,
    )
}

// PaymentEntryScreen
@Composable
fun PaymentEntryScreen(
    paymentValue: String,
    primaryFiatCurrency: String,
    exchangeRate: Double?,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onSettingsClick: () -> Unit,
    openSettingsPinCodeDialog: Boolean,
    pinCodeOpenSettings: String,
    updateOpenSettingsPinCodeDialog: (Boolean) -> Unit,
    openSettings: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column (
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(innerPadding).fillMaxHeight()
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = {onSettingsClick()},
                ) {
                    Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
            Box(
                modifier = Modifier.padding(horizontal = 44.dp).padding(bottom = 16.dp),
            ) {
                CurrencyConverterCard(
                    currency = primaryFiatCurrency,
                    exchangeRate = exchangeRate,
                    paymentValue = paymentValue,
                )
            }
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .wrapContentHeight()
            ) {

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                PaymentValue(value = paymentValue, currency = primaryFiatCurrency)
                Spacer(modifier = Modifier.height(8.dp))
                Box (
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) { PaymentEntryButtons(
                    onDigitClick = onDigitClick,
                ) }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
                Spacer(modifier = Modifier.height(24.dp))
                PaymentEntryControlButtons(
                    onBackspaceClick = onBackspaceClick,
                    onClearClick = onClearClick,
                    onSubmitClick = onSubmitClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    when {
        openSettingsPinCodeDialog -> {
            OpenSettingsDialog(
                onDismissRequest = { updateOpenSettingsPinCodeDialog(false) },
                onConfirmation = {
                    openSettings()
                },
                pinCode = pinCodeOpenSettings,
            )
        }
    }
}

@Composable
fun OpenSettingsDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    pinCode: String
) {
    var currentPinCode by remember { mutableStateOf("") }
    AlertDialog(
        icon = {
            Icon(Icons.Default.Lock, contentDescription = "Locked")
        },
        title = {
            Text(text = "Settings locked")
        },
        text = {
            Column {
                TextField(
                    value = currentPinCode,
                    onValueChange = {currentPinCode = it},
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Enter your PIN") }
                )
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentPinCode == pinCode) {
                        onConfirmation()
                    }
                }
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Go back")
            }
        }
    )
}

// PaymentValue
@Composable
fun PaymentValue(value: String, currency: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 32.dp,
        modifier = Modifier.padding(16.dp).fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp)

        ) {
            Text(text = currency, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// PaymentEntryButtons
@Composable
fun PaymentEntryButtons(
    onDigitClick: (String) -> Unit,
) {
    Column {
        Row {
            PaymentEntryButton(
                text = "1",
                onClick = { onDigitClick("1") },
                modifier = Modifier.weight(1f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = "2",
                onClick = { onDigitClick("2") },
                modifier = Modifier.weight(1f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = "3",
                onClick = { onDigitClick("3") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            PaymentEntryButton(
                text = "4",
                onClick = { onDigitClick("4") },
                modifier = Modifier.weight(1f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = "5",
                onClick = { onDigitClick("5") },
                modifier = Modifier.weight(1f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = "6",
                onClick = { onDigitClick("6") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row {
            PaymentEntryButton(
                text = "7",
                onClick = { onDigitClick("7") },
                modifier = Modifier.weight(1f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = "8",
                onClick = { onDigitClick("8") },
                modifier = Modifier.weight(1f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = "9",
                onClick = { onDigitClick("9") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row (
            modifier = Modifier.fillMaxWidth()
        ) {
            PaymentEntryButton(
                text = "0",
                onClick = { onDigitClick("0") },
                modifier = Modifier.weight(2f)
            )
            ButtonSpacing()
            PaymentEntryButton(
                text = ".",
                onClick = { onDigitClick(".") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ButtonSpacing() {
    Spacer(
        modifier = Modifier.padding(horizontal = 10.dp)
    )
}

@Composable
fun PaymentEntryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        border = ButtonDefaults.outlinedButtonBorder().copy(width = 1.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.outlinedButtonColors().copy(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.height(72.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

// PaymentEntryControlButtons (backspace, clear, and forward)
@Composable
fun PaymentEntryControlButtons(
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PaymentEntryControlButton(
            icon = Icons.Rounded.Clear,
            contentDescription = "Clear",
            onClick = onClearClick,
            containerColor = Color(0xFF770000),
            modifier = Modifier.weight(1f)
        )
        PaymentEntryControlButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            onClick = onBackspaceClick,
            containerColor = Color(0xFF797000), // Yellow
            modifier = Modifier.weight(1f)
        )
        PaymentEntryControlButton(
            icon = Icons.Rounded.Done,
            contentDescription = "Done",
            onClick = onSubmitClick,
            containerColor = Color(0xFF1C6E00),
            modifier = Modifier.weight(1f)
        )
    }
}

// PaymentEntryControlButton
@Composable
fun PaymentEntryControlButton(
    icon: ImageVector,
    contentDescription: String?,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = containerColor
        ),
        modifier = modifier.height(72.dp).padding(horizontal = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

