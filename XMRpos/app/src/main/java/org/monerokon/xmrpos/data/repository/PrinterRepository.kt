package org.monerokon.xmrpos.data.repository

import kotlinx.coroutines.flow.first
import org.monerokon.xmrpos.data.printer.PrinterServiceManager
import org.monerokon.xmrpos.ui.PaymentSuccess
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PrinterRepository(private val printerServiceManager: PrinterServiceManager,
    private val dataStoreRepository: DataStoreRepository,
    private val storageRepository: StorageRepository) {

    fun bindPrinterService() {
        printerServiceManager.bindPrinterService()
    }

    fun unbindPrinterService() {
        printerServiceManager.unbindPrinterService()
    }

    suspend fun printReceipt(paymentSuccess: PaymentSuccess) {
        val companyLogo = storageRepository.readImage("logo.png")
        if (companyLogo != null) {
            printerServiceManager.printPicture(companyLogo)
            printerServiceManager.printSpacer()
        }
        printerServiceManager.printTextCenter(dataStoreRepository.getCompanyName().first())
        printerServiceManager.printTextCenter(dataStoreRepository.getContactInformation().first())
        printerServiceManager.printSpacer()
        printerServiceManager.printText("Date: ${parseIsoDate(paymentSuccess.timestamp)}")
        printerServiceManager.printText("Time: ${parseIsoTime(paymentSuccess.timestamp)}")
        printerServiceManager.printSpacer()
        printerServiceManager.printTextCenter("PURCHASE")
        printerServiceManager.printText("TXID: ${paymentSuccess.txId}")
        printerServiceManager.printText("XMR: ${paymentSuccess.xmrAmount}")
        printerServiceManager.printText("${paymentSuccess.primaryFiatCurrency}: ${paymentSuccess.fiatAmount}")
        printerServiceManager.printText("Exchange rate: ${paymentSuccess.exchangeRate} ${paymentSuccess.primaryFiatCurrency} / XMR")
        printerServiceManager.printSpacer()
        printerServiceManager.printTextCenter(dataStoreRepository.getReceiptFooter().first())
        printerServiceManager.printEnd()
    }

    private fun parseIsoDate(isoDate: String): String {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val localDateTime = LocalDateTime.parse(isoDate, formatter)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return localDateTime.format(dateFormatter)
    }

    private fun parseIsoTime(isoDate: String): String {
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val localDateTime = LocalDateTime.parse(isoDate, formatter)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return localDateTime.format(timeFormatter)
    }
}