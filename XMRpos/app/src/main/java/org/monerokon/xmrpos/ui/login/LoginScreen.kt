// LoginScreen.kt
package org.monerokon.xmrpos.ui.payment.login

import CurrencyConverterCard
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.common.composables.CustomAlertDialog

// LoginScreenRoot
@Composable
fun LoginScreenRoot(viewModel: LoginViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    LoginScreen(
        instanceUrl = viewModel.instanceUrl,
        vendorID = viewModel.vendorID,
        username = viewModel.username,
        password = viewModel.password,
        errorMessage = viewModel.errorMessage,
        inProgress = viewModel.inProgress,
        updateInstanceUrl = viewModel::updateInstanceUrl,
        updateVendorID = viewModel::updateVendorID,
        updateUsername = viewModel::updateUsername,
        updatePassword = viewModel::updatePassword,
        resetErrorMessage = viewModel::resetErrorMessage,
        loginPressed = viewModel::loginPressed,
    )
}

// LoginScreen
@Composable
fun LoginScreen(
    instanceUrl: String,
    vendorID: String,
    username: String,
    password: String,
    errorMessage: String,
    inProgress: Boolean,
    updateInstanceUrl: (String) -> Unit,
    updateVendorID: (String) -> Unit,
    updateUsername: (String) -> Unit,
    updatePassword: (String) -> Unit,
    resetErrorMessage: () -> Unit,
    loginPressed: () -> Unit

) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(innerPadding).fillMaxHeight().fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Text("Login to POS account", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(48.dp))
            TextField(value = instanceUrl, placeholder = { Text("Instance url") }, onValueChange = {updateInstanceUrl(it)})
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = vendorID, placeholder = { Text("Vendor id") }, onValueChange = {updateVendorID(it)}, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = username, placeholder = { Text("Username") }, onValueChange = {updateUsername(it)})
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = password, placeholder = { Text("Password") }, onValueChange = {updatePassword(it)}, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password))
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {loginPressed()},
            ) {
                Row {
                    Text("Login")
                    when {
                        inProgress -> {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                        }
                    }
                }
            }
        }
        when {
            errorMessage != "" -> {
                CustomAlertDialog(
                    onDismissRequest = { resetErrorMessage() },
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

    }
}

