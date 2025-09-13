package org.monerokon.xmrpos.ui.settings.backend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.common.composables.CustomAlertDialog
import org.monerokon.xmrpos.ui.settings.backend.BackendViewModel

@Composable
fun BackendScreenRoot(viewModel: BackendViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    BackendScreen(
        onBackClick = viewModel::navigateToMainSettings,
        confOptions = viewModel.confOptions,
        instanceUrl = viewModel.instanceUrl,
        requestInterval = viewModel.requestInterval,
        conf = viewModel.conf,
        updateRequestInterval = viewModel::updateRequestInterval,
        updateConf = viewModel::updateConf,
        healthStatus = viewModel.healthStatus,
        fetchBackendHealth = viewModel::fetchBackendHealth,
        resetHealthStatus = viewModel::resetHealthStatus,
        logout = viewModel::logout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackendScreen(
    onBackClick: () -> Unit,
    confOptions: List<String>,
    instanceUrl: String,
    requestInterval: String,
    conf: String,
    updateRequestInterval: (String) -> Unit,
    updateConf: (String) -> Unit,
    healthStatus: String,
    fetchBackendHealth: () -> Unit,
    resetHealthStatus: () -> Unit,
    logout: () -> Unit
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
                    Text("Backend")
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
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request interval", style = MaterialTheme.typography.labelLarge)
                TextField(
                    value = requestInterval,
                    onValueChange = {updateRequestInterval(it)},
                    label = { Text("Seconds") },
                    modifier = Modifier.width(130.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            ConfSelector(conf, confOptions, onConfSelected = {updateConf(it)}, modifier = Modifier.width(130.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = {fetchBackendHealth()}) {
                    Text("Check health")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = {logout()}) {
                    Text("Log out")
                }
            }
        }
        when {healthStatus != "" ->
            CustomAlertDialog(
                onDismissRequest = { resetHealthStatus() },
                onConfirmation = { resetHealthStatus() },
                dialogTitle = "Health status",
                dialogText = healthStatus,
                confirmButtonText = "OK",
                dismissButtonText = null,
                icon = Icons.Default.Info
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfSelector(value: String, confs: List<String>, onConfSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Number of confirmations\nto mark as paid", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {expanded = !expanded}, modifier = modifier) {
                TextField(
                    modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    value = value,
                    enabled = true,
                    label = { Text("Conf") },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded,
                        )
                    }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = {expanded = false}) {
                    confs.forEach { conf ->
                        DropdownMenuItem(
                            text = { Text(conf) },
                            onClick = {
                                expanded = false
                                onConfSelected(conf)
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
        }
    }
}