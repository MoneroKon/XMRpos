// CompanyInformationScreen.kt
package org.monerokon.xmrpos.ui.settings.companyinformation

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.shared.DisplayImageFromFile
import java.io.File

@Composable
fun CompanyInformationScreenRoot(viewModel: CompanyInformationViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    CompanyInformationScreen(
        onBackClick = viewModel::navigateToMainSettings,
        companyLogo = viewModel.companyLogo,
        companyName = viewModel.companyName,
        updateCompanyName = viewModel::updateCompanyName,
        contactInformation = viewModel.contactInformation,
        updateContactInformation = viewModel::updateContactInformation,
        receiptFooter = viewModel.receiptFooter,
        updateReceiptFooter = viewModel::updateReceiptFooter,
        saveLogo = viewModel::saveLogo,
        deleteLogo = viewModel::deleteLogo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyInformationScreen(
    onBackClick: () -> Unit,
    companyLogo: File?,
    companyName: String,
    updateCompanyName: (String) -> Unit,
    contactInformation: String,
    updateContactInformation: (String) -> Unit,
    receiptFooter: String,
    updateReceiptFooter: (String) -> Unit,
    saveLogo: (Uri) -> Unit,
    deleteLogo: () -> Unit
) {

    // Registers a photo picker activity launcher in single-select mode.
    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            saveLogo(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

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
                    Text("Company Information")
                }
            )
        },
    ) { innerPadding ->
        Column (
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(innerPadding).padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalIconButton (
                    onClick = { if (companyLogo == null) {pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))} else {deleteLogo()} },
                    modifier = Modifier.size(100.dp)
                ) {
                    if (companyLogo != null) {
                        DisplayImageFromFile(companyLogo)
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete company logo",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload company logo",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Upload logo", style = MaterialTheme.typography.titleLarge)
                    Text("This logo will be shown on the printed receipts", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = companyName,
                onValueChange = {updateCompanyName(it)},
                label = { Text("Company name") },
                supportingText = { Text("Shown on the receipts") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = contactInformation,
                onValueChange = {updateContactInformation(it)},
                label = { Text("Contact information") },
                supportingText = { Text("Shown on the receipts") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextField(
                value = receiptFooter,
                onValueChange = {updateReceiptFooter(it)},
                label = { Text("Receipt footer") },
                supportingText = { Text("Shown at the end of the receipt") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}