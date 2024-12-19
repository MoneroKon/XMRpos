package org.monerokon.xmrpos.data.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import net.nyx.printerservice.print.IPrinterService
import net.nyx.printerservice.print.PrintTextFormat
import java.io.File

class PrinterServiceManager(private val context: Context) {

    private var printerService: IPrinterService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            printerService = IPrinterService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            printerService = null
        }
    }

    fun bindPrinterService() {
        val intent = Intent().apply {
            setPackage("net.nyx.printerservice")
            action = "net.nyx.printerservice.IPrinterService"
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindPrinterService() {
        context.unbindService(serviceConnection)
    }

    fun printText(text: String) {
        printerService?.let {
            val format = PrintTextFormat()
            // Set formatting options for text here (optional)
            it.printText(text, format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printTextCenter(text: String) {
        printerService?.let {
            val format = PrintTextFormat().apply {
                ali = 1
            }
            it.printText(text, format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printSpace() {
        printerService?.let {
            val format = PrintTextFormat().apply {
                topPadding = 10
            }
            it.printText("", format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printSpacer() {
        printerService?.let {
            val format = PrintTextFormat().apply {
                ali = 1
                topPadding = 5
            }
            it.printText("------------------------------------------------", format)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printEnd() {
        printerService?.printEndAutoOut() ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
    }

    fun printPicture(picture: File) {
        printerService?.let {
            val bitmap = BitmapFactory.decodeFile(picture.absolutePath)
            val scaledBitmap = scaleBitmapToMaxSize(bitmap)
            // make bitmap fit 1/4 with of paper
            it.printBitmap(scaledBitmap, 1, 1)
        } ?: run {
            Log.e("PrinterServiceManager", "Printer Service not connected")
        }
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
