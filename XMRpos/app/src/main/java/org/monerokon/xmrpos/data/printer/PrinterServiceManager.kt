package org.monerokon.xmrpos.data.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbManager
import android.util.Log
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.tcp.TcpConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import kotlinx.coroutines.flow.first
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import java.io.File

class PrinterServiceManager(private val context: Context, private val dataStoreRepository: DataStoreRepository) {

    var printer: EscPosPrinter? = null

    var printFormattedString: String? = ""

    var nbrCharactersPerLine: Int? = null

    var lastConfig = ""

    suspend fun updateEscPosPrinter(): Boolean {
        printFormattedString = ""
        val newPrinterType = dataStoreRepository.getPrinterConnectionType().first()
        val newPrinterDpi = dataStoreRepository.getPrinterDpi().first()
        val newPrinterWidth = dataStoreRepository.getPrinterWidth().first()
        val newPrinterNbrCharactersPerLine = dataStoreRepository.getPrinterNbrCharactersPerLine().first()
        nbrCharactersPerLine = newPrinterNbrCharactersPerLine
        val newPrinterCharsetEncoding = dataStoreRepository.getPrinterCharsetEncoding().first()
        val newPrinterCharsetId = dataStoreRepository.getPrinterCharsetId().first()
        val newPrinterAddress = dataStoreRepository.getPrinterAddress().first()
        val newPrinterPort = dataStoreRepository.getPrinterPort().first()

        val newConfig = newPrinterType + newPrinterDpi + newPrinterWidth + newPrinterNbrCharactersPerLine + newPrinterCharsetEncoding + newPrinterCharsetId + newPrinterAddress + newPrinterPort

        if (lastConfig == newConfig) {
            Log.i("PrinterServiceManager", "Printer configuration is the same as last time")
            return true
        }

        // Bluetooth
        if (newPrinterType == "bluetooth") {
            printer = EscPosPrinter(
                BluetoothPrintersConnections.selectFirstPaired(),
                newPrinterDpi,
                newPrinterWidth.toFloat(),
                newPrinterNbrCharactersPerLine,
                EscPosCharsetEncoding(newPrinterCharsetEncoding, newPrinterCharsetId)
            )
            lastConfig = newPrinterType + newPrinterDpi + newPrinterWidth + newPrinterNbrCharactersPerLine + newPrinterCharsetEncoding + newPrinterCharsetId + newPrinterAddress + newPrinterPort
            return true;
        }

        // TCP/IP
        if (newPrinterType == "tcp/ip") {
            printer = EscPosPrinter(
                TcpConnection(newPrinterAddress, newPrinterPort),
                newPrinterDpi,
                newPrinterWidth.toFloat(),
                newPrinterNbrCharactersPerLine,
                EscPosCharsetEncoding(newPrinterCharsetEncoding, newPrinterCharsetId)
            )
            lastConfig = newPrinterType + newPrinterDpi + newPrinterWidth + newPrinterNbrCharactersPerLine + newPrinterCharsetEncoding + newPrinterCharsetId + newPrinterAddress + newPrinterPort
            return true;
        }

        // USB
        if (newPrinterType == "usb") {
            val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context)
            val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            val device = usbConnection?.device

            printer = EscPosPrinter(
                UsbConnection(usbManager, device),
                newPrinterDpi,
                newPrinterWidth.toFloat(),
                newPrinterNbrCharactersPerLine,
                EscPosCharsetEncoding(newPrinterCharsetEncoding, newPrinterCharsetId)
            )
            lastConfig = newPrinterType + newPrinterDpi + newPrinterWidth + newPrinterNbrCharactersPerLine + newPrinterCharsetEncoding + newPrinterCharsetId + newPrinterAddress + newPrinterPort
            return true;
        }

        return false
    }

    fun printEnd() {
        printFormattedString?.removeSuffix("\n")
        printer?.printFormattedTextAndCut(printFormattedString, 5f)
        printFormattedString = ""
    }

    fun printText(text: String) {
        printFormattedString += "[L]$text\n"
    }

    fun printTextCenter(text: String) {
        printFormattedString += "[C]$text\n"
    }

    fun printSpacer() {
        val spacer = "-".repeat(nbrCharactersPerLine ?: 30)
        printFormattedString += "[C]$spacer\n"
    }

    fun printPicture(picture: File) {
        Log.i("PrinterServiceManager", "Printing picture: ${picture.absolutePath}")
        val bitmap = BitmapFactory.decodeFile(picture.absolutePath)

        val scaledBitmap = scaleBitmapToMaxSize(bitmap)

        val finalBitmap = bitmapTransparentToWhite(scaledBitmap)

        printFormattedString += "[C]<img>${PrinterTextParserImg.bitmapToHexadecimalString(printer, finalBitmap)}</img>\n"

        Log.i("PrinterServiceManager", "Printing picture done")
    }

    private fun bitmapTransparentToWhite(bitmap: Bitmap): Bitmap {
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until newBitmap.width) {
            for (y in 0 until newBitmap.height) {
                val pixel = newBitmap.getPixel(x, y)
                if (pixel == 0) {
                    newBitmap.setPixel(x, y, -1)
                }
            }
        }
        return newBitmap
    }
    private fun scaleBitmapToMaxSize(bitmap: Bitmap, maxWidth: Int = 160, maxHeight: Int = 80): Bitmap {
        // Calculate the aspect ratio
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

        // Calculate new dimensions based on the max width and height
        var newWidth: Int
        var newHeight: Int

        if (bitmap.width > bitmap.height) {
            // If the width is greater than the height, scale based on maxWidth
            newWidth = maxWidth
            newHeight = (newWidth / aspectRatio).toInt()
        } else {
            // If the height is greater than the width, scale based on maxHeight
            newHeight = maxHeight
            newWidth = (newHeight * aspectRatio).toInt()
        }

        // If either dimension exceeds the maximum size, adjust the other dimension
        if (newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (newHeight * aspectRatio).toInt()
        }

        if (newWidth > maxWidth) {
            newWidth = maxWidth
            newHeight = (newWidth / aspectRatio).toInt()
        }

        // Create and return the scaled bitmap
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }

}