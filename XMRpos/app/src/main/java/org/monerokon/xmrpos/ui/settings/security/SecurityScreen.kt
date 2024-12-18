// MoneroPayScreen.kt
package org.monerokon.xmrpos.ui.settings.moneropay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SecurityScreenRoot(viewModel: SecurityViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    SecurityScreen(
        onBackClick = viewModel::navigateToMainSettings,
        requirePinCodeOnAppStart = viewModel.requirePinCodeOnAppStart,
        requirePinCodeOpenSettings = viewModel.requirePinCodeOpenSettings,
        pinCodeOnAppStart = viewModel.pinCodeOnAppStart,
        pinCodeOpenSettings = viewModel.pinCodeOpenSettings,
        updateRequirePinCodeOnAppStart = viewModel::updateRequirePinCodeOnAppStart,
        updateRequirePinCodeOpenSettings = viewModel::updateRequirePinCodeOpenSettings,
        updatePinCodeOnAppStart = viewModel::updatePinCodeOnAppStart,
        updatePinCodeOpenSettings = viewModel::updatePinCodeOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onBackClick: () -> Unit,
    requirePinCodeOnAppStart: Boolean,
    requirePinCodeOpenSettings: Boolean,
    pinCodeOnAppStart: String,
    pinCodeOpenSettings: String,
    updateRequirePinCodeOnAppStart: (Boolean) -> Unit,
    updateRequirePinCodeOpenSettings: (Boolean) -> Unit,
    updatePinCodeOnAppStart: (String) -> Unit,
    updatePinCodeOpenSettings: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = {onBackClick()}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go back to previous screen"
                        )
                    }
                },
                title = {
                    Text("Security")
                }
            )
        },
    ) { innerPadding ->
        Column (
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
           Row (
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
           ) {
                Text("Require PIN code on app start", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = requirePinCodeOnAppStart,
                    onCheckedChange = { updateRequirePinCodeOnAppStart(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row (
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Require PIN code to open settings", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = requirePinCodeOpenSettings,
                    onCheckedChange = { updateRequirePinCodeOpenSettings(it) }
                )
           }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Column (
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PIN code on app start", style = MaterialTheme.typography.labelLarge)
                    TextField(
                        enabled = requirePinCodeOnAppStart,
                        value = pinCodeOnAppStart,
                        onValueChange = { updatePinCodeOnAppStart(it) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Enter PIN code") }
                    )
                }
                Spacer(modifier = Modifier.width(42.dp))
                Column (
                    modifier = Modifier.weight(1f)
                ){
                    Text("PIN code to open settings", style = MaterialTheme.typography.labelLarge)
                    // only number input
                    TextField(
                        enabled = requirePinCodeOpenSettings,
                        value = pinCodeOpenSettings,
                        onValueChange = { updatePinCodeOpenSettings(it) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Enter PIN code") }
                    )
                }
            }
        }}
}

