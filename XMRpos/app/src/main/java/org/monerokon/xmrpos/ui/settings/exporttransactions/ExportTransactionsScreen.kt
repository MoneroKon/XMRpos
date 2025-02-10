package org.monerokon.xmrpos.ui.settings.exporttransactions

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.sharp.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun ExportTransactionsScreenRoot(viewModel: ExportTransactionsViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    ExportTransactionsScreen(
        onBackClick = viewModel::navigateToMainSettings,
        exportAllTransactions = viewModel::exportAllTransactions,
        deleteAllTransactions = viewModel::deleteAllTransactions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTransactionsScreen(
    onBackClick: () -> Unit,
    exportAllTransactions: (Context, Uri) -> Unit,
    deleteAllTransactions: () -> Unit,
) {
    var showAreYouSure by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Create document launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            // Trigger the export logic with the selected URI
            exportAllTransactions(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = { onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Go back to previous screen"
                        )
                    }
                },
                title = {
                    Text("Export transactions")
                }
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Press the button below to export all transactions to a CSV file.")
            FilledTonalButton(onClick = {createDocumentLauncher.launch("database_export.csv")}) {
                Text("Export as CSV in Koinly format")
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Delete all the transactions saved on this device.")
            // should be red button
            Button(onClick = { showAreYouSure = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Delete all transactions")
            }
        }
        if (showAreYouSure)
            AreYouSureDialog(
                onDismissRequest = { showAreYouSure = false },
                onConfirmation = { showAreYouSure = false; deleteAllTransactions() },
            )
    }
}

@Composable
fun AreYouSureDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            // warning icon
            Icon(imageVector = Icons.Sharp.Warning, contentDescription = "Warning")
        },
        title = {
            Text(text = "Are you sure?")
        },
        text = {
            Text(text = "This action cannot be undone. You will delete all transactions saved on this device.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}




