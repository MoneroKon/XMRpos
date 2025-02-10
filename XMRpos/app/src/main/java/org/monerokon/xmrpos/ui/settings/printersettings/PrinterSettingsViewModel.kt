package org.monerokon.xmrpos.ui.settings.printersettings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.PrinterRepository
import org.monerokon.xmrpos.ui.PaymentSuccess
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val printerRepository: PrinterRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val logTag = "PrinterSettingsViewModel"

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    val manager: UsbManager = ContextCompat.getSystemService(context, UsbManager::class.java) as UsbManager

    var printerConnectionType: String by mutableStateOf(" ")
    var printerDpi: String by mutableStateOf(" ")
    var printerWidth: String by mutableStateOf(" ")
    var printerNbrCharactersPerLine by mutableStateOf(" ")
    var printerCharsetEncoding by mutableStateOf(" ")
    var printerCharsetId by mutableStateOf(" ")
    var printerAddress by mutableStateOf("")
    var printerPort by mutableStateOf(" ")
    var attachedUsbDevices by mutableStateOf<List<UsbDevice>>(emptyList())

    var printingInProgress by mutableStateOf(false)

    init {
        viewModelScope.launch {
            dataStoreRepository.getPrinterConnectionType().collect { storedPrinterConnectionType ->
                printerConnectionType = storedPrinterConnectionType
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterDpi().collect { storedPrinterDpi ->
                printerDpi = storedPrinterDpi.toString()
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterWidth().collect { storedPrinterWidth ->
                printerWidth = storedPrinterWidth.toString()
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterNbrCharactersPerLine().collect { storedPrinterNbrCharactersPerLine ->
                printerNbrCharactersPerLine = storedPrinterNbrCharactersPerLine.toString()
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterCharsetEncoding().collect { storedPrinterCharsetEncoding ->
                printerCharsetEncoding = storedPrinterCharsetEncoding
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterCharsetId().collect { storedPrinterCharsetId ->
                printerCharsetId = storedPrinterCharsetId.toString()
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterAddress().collect { storedPrinterAddress ->
                printerAddress = storedPrinterAddress
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPrinterPort().collect { storedPrinterPort ->
                printerPort = storedPrinterPort.toString()
            }
        }

        updateAttachedUsbDevices()
    }

    private fun updateAttachedUsbDevices() {
        val deviceList: HashMap<String, UsbDevice> = manager.deviceList
        attachedUsbDevices = deviceList.values.toList()
        Log.i(logTag, "Attached USB devices: $attachedUsbDevices")
    }

    val visiblePermissionDialogQueue = mutableStateListOf<String>()

    fun dismissDialog() {
        visiblePermissionDialogQueue.removeAt(0)
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if(!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
            visiblePermissionDialogQueue.add(permission)
        }
    }

    fun updatePrinterConnectionType(newPrinterConnectionType: String) {
        printerConnectionType = newPrinterConnectionType
        viewModelScope.launch {
            dataStoreRepository.savePrinterConnectionType(newPrinterConnectionType)
        }
    }

    fun updatePrinterDpi(newPrinterDpi: String) {
        printerDpi = newPrinterDpi
        newPrinterDpi.toIntOrNull()?.let {
            viewModelScope.launch {
                dataStoreRepository.savePrinterDpi(it)
            }
        }
    }

    fun updatePrinterWidth(newPrinterWidth: String) {
        printerWidth = newPrinterWidth
        newPrinterWidth.toIntOrNull()?.let {
            viewModelScope.launch {
                dataStoreRepository.savePrinterWidth(it)
            }
        }
    }

    fun updatePrinterNbrCharactersPerLine(newPrinterNbrCharactersPerLine: String) {
        printerNbrCharactersPerLine = newPrinterNbrCharactersPerLine
        newPrinterNbrCharactersPerLine.toIntOrNull()?.let {
            viewModelScope.launch {
                dataStoreRepository.savePrinterNbrCharactersPerLine(it)
            }
        }
    }

    fun updatePrinterCharsetEncoding(newPrinterCharsetEncoding: String) {
        printerCharsetEncoding = newPrinterCharsetEncoding
        viewModelScope.launch {
            dataStoreRepository.savePrinterCharsetEncoding(newPrinterCharsetEncoding)
        }
    }

    fun updatePrinterCharsetId(newPrinterCharsetId: String) {
        printerCharsetId = newPrinterCharsetId
        newPrinterCharsetId.toIntOrNull()?.let {
            viewModelScope.launch {
                dataStoreRepository.savePrinterCharsetId(it)
            }
        }
    }

    fun updatePrinterAddress(newPrinterAddress: String) {
        printerAddress = newPrinterAddress
        viewModelScope.launch {
            dataStoreRepository.savePrinterAddress(newPrinterAddress)
        }
    }

    fun updatePrinterPort(newPrinterPort: String) {
        printerPort = newPrinterPort
        newPrinterPort.toIntOrNull()?.let {
            viewModelScope.launch {
                dataStoreRepository.savePrinterPort(it)
            }
        }
    }

    fun requestPermissionPrinterUsb(newDevice: UsbDevice) {
        val usbManager: UsbManager = ContextCompat.getSystemService(context, UsbManager::class.java) as UsbManager
        val ACTION_USB_PERMISSION = "org.monerokon.xmrpos.USB_PERMISSION"
        val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)
        usbManager.requestPermission(newDevice, permissionIntent)
    }

    fun printTest() {
        printingInProgress = true
        CoroutineScope(Dispatchers.IO).launch {
            printerRepository.printReceipt(PaymentSuccess(
                fiatAmount = 1.0,
                primaryFiatCurrency = "USD",
                txId = "2530548aeba36c80d2c856274a587678e191661a20f5a8abf3e51e0123cb8797",
                xmrAmount = 0.012039402,
                exchangeRate = 201.3,
                timestamp = "1970-01-01T00:00:00Z",
                showPrintReceipt = true
            ))
            printingInProgress = false
        }
    }
}