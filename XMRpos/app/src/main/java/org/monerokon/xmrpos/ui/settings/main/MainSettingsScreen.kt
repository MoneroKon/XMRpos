// MainSettingsScreen.kt
package org.monerokon.xmrpos.ui.settings.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MainSettingsScreenRoot(viewModel: MainSettingsViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    MainSettingsScreen(
        onBackClick = viewModel::navigateToPayment,
        navigateToCompanyInformation = viewModel::navigateToCompanyInformation,
        navigateToFiatCurrencies = viewModel::navigateToFiatCurrencies,
        navigateToSecurity = viewModel::navigateToSecurity,
        navigateToTransactionHistory = viewModel::navigateToTransactionHistory,
        navigateToBackend = viewModel::navigateToBackend,
        navigateToPrinterSettings = viewModel::navigateToPrinterSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onBackClick: () -> Unit,
    navigateToCompanyInformation: () -> Unit,
    navigateToFiatCurrencies: () -> Unit,
    navigateToSecurity: () -> Unit,
    navigateToTransactionHistory: () -> Unit,
    navigateToBackend: () -> Unit,
    navigateToPrinterSettings: () -> Unit
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
                    Text("Settings")
                }
            )
        },
    ) { innerPadding ->
        Column (
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            SettingsCard(text = "Company information", onClick = {navigateToCompanyInformation()})
            Spacer(modifier = Modifier.height(24.dp))
            SettingsCard(text = "Fiat currencies", onClick = {navigateToFiatCurrencies()})
            Spacer(modifier = Modifier.height(24.dp))
            SettingsCard(text = "Security", onClick = {navigateToSecurity()})
            Spacer(modifier = Modifier.height(24.dp))
            SettingsCard(text = "Transaction history", onClick = {navigateToTransactionHistory()})
            Spacer(modifier = Modifier.height(24.dp))
            SettingsCard(text = "Backend", onClick = {navigateToBackend()})
            Spacer(modifier = Modifier.height(24.dp))
            SettingsCard(text = "Printer settings", onClick = {navigateToPrinterSettings()})
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    text: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

