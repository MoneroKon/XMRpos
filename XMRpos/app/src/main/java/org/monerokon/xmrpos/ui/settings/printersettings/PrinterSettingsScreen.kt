package org.monerokon.xmrpos.ui.settings.printersettings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.common.composables.BluetoothConnectPermissionTextProvider
import org.monerokon.xmrpos.ui.common.composables.BluetoothScanPermissionTextProvider
import org.monerokon.xmrpos.ui.common.composables.PermissionDialog

@Composable
fun PrinterSettingsScreenRoot(viewModel: PrinterSettingsViewModel, navController: NavHostController) {
    viewModel.setNavController(navController)
    PrinterSettingsScreen(
        onBackClick = viewModel::navigateToMainSettings,
        dismissDialog = viewModel::dismissDialog,
        onPermissionResult = viewModel::onPermissionResult,
        visiblePermissionDialogQueue = viewModel.visiblePermissionDialogQueue,
        printerConnectionType = viewModel.printerConnectionType,
        updatePrinterConnectionType = viewModel::updatePrinterConnectionType,
        printerDpi = viewModel.printerDpi,
        updatePrinterDpi = viewModel::updatePrinterDpi,
        printerWidth = viewModel.printerWidth,
        updatePrinterWidth = viewModel::updatePrinterWidth,
        printerNbrCharactersPerLine = viewModel.printerNbrCharactersPerLine,
        updatePrinterNbrCharactersPerLine = viewModel::updatePrinterNbrCharactersPerLine,
        printerCharsetEncoding = viewModel.printerCharsetEncoding,
        updatePrinterCharsetEncoding = viewModel::updatePrinterCharsetEncoding,
        printerCharsetId = viewModel.printerCharsetId,
        updatePrinterCharsetId = viewModel::updatePrinterCharsetId,
        printerAddress = viewModel.printerAddress,
        updatePrinterAddress = viewModel::updatePrinterAddress,
        printerPort = viewModel.printerPort,
        updatePrinterPort = viewModel::updatePrinterPort,
        requestPermissionPrinterUsb = viewModel::requestPermissionPrinterUsb,
        attachedUsbDevices = viewModel.attachedUsbDevices,
        printTest = viewModel::printTest,
        printingInProgress = viewModel.printingInProgress
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    onBackClick: () -> Unit,
    dismissDialog: () -> Unit,
    onPermissionResult: (String, Boolean) -> Unit,
    visiblePermissionDialogQueue: List<String>,
    printerConnectionType: String,
    updatePrinterConnectionType: (String) -> Unit,
    printerDpi: String,
    updatePrinterDpi: (String) -> Unit,
    printerWidth: String,
    updatePrinterWidth: (String) -> Unit,
    printerNbrCharactersPerLine: String,
    updatePrinterNbrCharactersPerLine: (String) -> Unit,
    printerCharsetEncoding: String,
    updatePrinterCharsetEncoding: (String) -> Unit,
    printerCharsetId: String,
    updatePrinterCharsetId: (String) -> Unit,
    printerAddress: String,
    updatePrinterAddress: (String) -> Unit,
    printerPort: String,
    updatePrinterPort: (String) -> Unit,
    requestPermissionPrinterUsb: (UsbDevice) -> Unit,
    attachedUsbDevices: List<UsbDevice>,
    printTest: () -> Unit,
    printingInProgress: Boolean
) {
    val bluetoothPermissionsToRequest = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    val bluetoothPermissionResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            bluetoothPermissionsToRequest.forEach { permission ->
                onPermissionResult(
                    permission,
                    perms[permission] == true
                )
            }
        }
    )

    val activity = LocalContext.current as Activity
    val scrollState = rememberScrollState()

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
                    Text("Printer settings")
                }
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text("Printer connection", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                    onClick = {
                        updatePrinterConnectionType("bluetooth")
                        bluetoothPermissionResultLauncher.launch(bluetoothPermissionsToRequest)
                    },
                    selected = printerConnectionType == "bluetooth"
                ) {
                    Text("Bluetooth")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                    onClick = { updatePrinterConnectionType("tcp/ip") },
                    selected = printerConnectionType == "tcp/ip"
                ) {
                    Text("TCP/IP")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                    onClick = { updatePrinterConnectionType("usb") },
                    selected = printerConnectionType == "usb"
                ) {
                    Text("USB")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                    onClick = { updatePrinterConnectionType("none") },
                    selected = printerConnectionType == "none"
                ) {
                    Text("None")
                }
            }
            AnimatedVisibility(
                visible = printerConnectionType == "tcp/ip",
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    // TODO: REMOVE THIS WHEN FEATURE IS TESTED
                    Text("Untested feature", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextField(
                            value = printerAddress,
                            onValueChange = { updatePrinterAddress(it) },
                            label = { Text("IP address") },
                            modifier = Modifier.width(200.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TextField(
                            value = printerPort,
                            onValueChange = { updatePrinterPort(it) },
                            label = { Text("Port") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = printerConnectionType == "usb",
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    // TODO: REMOVE THIS WHEN FEATURE IS TESTED
                    Text("Untested feature", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    if (attachedUsbDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Attached USB devices", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        attachedUsbDevices.forEach { device ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text("Device name: ${device.deviceName}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Device ID: ${device.deviceId}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Vendor ID: ${device.vendorId}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Product ID: ${device.productId}")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            requestPermissionPrinterUsb(
                                                device
                                            )
                                        }
                                    ) {
                                        Text("Request permission")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No attached USB devices", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Printer parameters", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Printer DPI")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = printerDpi,
                            onValueChange = { updatePrinterDpi(it) },
                            label = { Text("DPI") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Printer width (mm)")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = printerWidth,
                            onValueChange = { updatePrinterWidth(it) },
                            label = { Text("mm") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Printer characters per line")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = printerNbrCharactersPerLine,
                            onValueChange = { updatePrinterNbrCharactersPerLine(it) },
                            label = { Text("Characters") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Printer charset encoding")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = printerCharsetEncoding,
                            onValueChange = { updatePrinterCharsetEncoding(it) },
                            label = { Text("Encoding") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Printer charset ID")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = printerCharsetId,
                            onValueChange = { updatePrinterCharsetId(it) },
                            label = { Text("ID") },
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            FilledTonalButton (
                onClick = { printTest() },
                enabled = !printingInProgress,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Print test")
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
            visiblePermissionDialogQueue
                .reversed()
                .forEach { permission ->
                    PermissionDialog(
                        permissionTextProvider = when (permission) {
                            Manifest.permission.BLUETOOTH -> {
                                BluetoothConnectPermissionTextProvider()
                            }
                            Manifest.permission.BLUETOOTH_SCAN -> {
                                BluetoothScanPermissionTextProvider()
                            }
                            Manifest.permission.BLUETOOTH_CONNECT -> {
                                BluetoothConnectPermissionTextProvider()
                            }
                            else -> return@forEach
                        },
                        isPermanentlyDeclined = !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity, permission
                        ),
                        onDismiss = dismissDialog,
                        onOkClick = {
                            dismissDialog()
                            bluetoothPermissionResultLauncher.launch(
                                arrayOf(permission)
                            )
                        },
                        onGoToAppSettingsClick = { activity.openAppSettings() }
                    )
                }
        }
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}