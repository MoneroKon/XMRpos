package org.monerokon.xmrpos.ui.common.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        icon = {
            Icon(imageVector = Icons.Sharp.Info, contentDescription = "Info")
        },
        title = {
            Text(text = "Permission required")
        },
        text = {
            Text(
                text = permissionTextProvider.getDescription(
                    isPermanentlyDeclined = isPermanentlyDeclined
                )
            )
        },
        onDismissRequest = {
            onDismiss()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isPermanentlyDeclined) {
                        onGoToAppSettingsClick()
                    } else {
                        onOkClick()
                    }
                }
            ) {
                Text(
                    text = if(isPermanentlyDeclined) {
                        "Grant permission"
                    } else {
                        "OK"
                    },
                )
            }
        },
        dismissButton = null,
        modifier = modifier
    )
}

interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class BluetoothPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs access to your Bluetooth so that it can interact with a printer connected via Bluetooth"
        }
    }
}

class BluetoothConnectPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth connect permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs access to your Bluetooth connect so that it can connect to your Bluetooth printer"
        }
    }
}

class BluetoothScanPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if(isPermanentlyDeclined) {
            "It seems you permanently declined Bluetooth scan permission. " +
                    "You can go to the app settings to grant it."
        } else {
            "This app needs Bluetooth scan permission so that it can scan for printer that you can connect to"
        }
    }
}