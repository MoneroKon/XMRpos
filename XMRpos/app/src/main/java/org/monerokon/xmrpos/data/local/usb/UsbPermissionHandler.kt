package org.monerokon.xmrpos.data.local.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

class UsbPermissionHandler {

    companion object {
        private const val TAG = "UsbPermissionHandler"
        const val ACTION_USB_PERMISSION = "org.monerokon.xmrpos.USB_PERMISSION"
    }

    val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION") // Suppress deprecation warning for older versions
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            // Call method to set up device communication
                            setupDeviceCommunication(this)
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device $device")
                    }
                }
            }
        }
    }

    private fun setupDeviceCommunication(device: UsbDevice) {
        Log.i(TAG, "Device $device is ready for communication")
    }
}
