package org.monerokon.xmrpos.data.hce

import android.app.Service
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class HceServiceManager : HostApduService() {

    private val logTag = "HceServiceManager"

    private val selectApdu = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(), 0x07.toByte(),
        0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(), 0x85.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte()
    )

    private val selectCapabilityContainer = byteArrayOf(
        0x00.toByte(), 0xa4.toByte(), 0x00.toByte(), 0x0c.toByte(), 0x02.toByte(), 0xe1.toByte(), 0x03.toByte()
    )

    private val readCapabilityContainerCmd = byteArrayOf(
        0x00.toByte(), 0xb0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0f.toByte()
    )

    private var isCapabilityContainerRead = false

    private val capabilityContainerResponse = byteArrayOf(
        0x00.toByte(), 0x11.toByte(), 0x20.toByte(), 0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(),
        0x04.toByte(), 0x06.toByte(), 0xE1.toByte(), 0x04.toByte(), 0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(),
        0x90.toByte(), 0x00.toByte()
    )

    private val selectNdef = byteArrayOf(
        0x00.toByte(), 0xa4.toByte(), 0x00.toByte(), 0x0c.toByte(), 0x02.toByte(), 0xE1.toByte(), 0x04.toByte()
    )

    private val readBinaryCmd = byteArrayOf(
        0x00.toByte(), 0xb0.toByte()
    )

    private val readBinaryNlenCmd = byteArrayOf(
        0x00.toByte(), 0xb0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x02.toByte()
    )

    private val successResponse = byteArrayOf(
        0x90.toByte(), 0x00.toByte()
    )

    private val errorResponse = byteArrayOf(
        0x6A.toByte(), 0x82.toByte()
    )

    private val waitResponse = byteArrayOf(
        0x90.toByte(), 0x00.toByte()
    )

    private var uriData = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("uri")?.let {
            uriData = it
        }

        Log.i(logTag, "onStartCommand - uri: $uriData")

        return Service.START_NOT_STICKY
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.i(logTag, "Received APDU: " + commandApdu.toHexString())

        if (uriData.isEmpty()) {
            Log.i(logTag, "uriData is empty. Returning wait response.")
            return waitResponse
        }

        val ndefMessage = NdefMessage(createUriRecord(uriData))
        val ndefBytes = ndefMessage.toByteArray()
        val ndefLength = ndefBytes.size.toByteArray(2)

        return when {
            selectApdu.contentEquals(commandApdu) -> {
                Log.i(logTag, "selectApdu matched. Response: " + successResponse.toHexString())
                successResponse
            }
            selectCapabilityContainer.contentEquals(commandApdu) -> {
                Log.i(logTag, "selectCapabilityContainer matched. Response: " + successResponse.toHexString())
                successResponse
            }
            readCapabilityContainerCmd.contentEquals(commandApdu) && !isCapabilityContainerRead -> {
                Log.i(logTag, "readCapabilityContainerCmd matched. Response: " + capabilityContainerResponse.toHexString())
                isCapabilityContainerRead = true
                capabilityContainerResponse
            }
            selectNdef.contentEquals(commandApdu) -> {
                Log.i(logTag, "selectNdef matched. Response: " + successResponse.toHexString())
                successResponse
            }
            readBinaryNlenCmd.contentEquals(commandApdu) -> {
                val response = ndefLength + successResponse
                Log.i(logTag, "readBinaryNlenCmd matched. Response: " + response.toHexString())
                isCapabilityContainerRead = false
                response
            }
            commandApdu.sliceArray(0..1).contentEquals(readBinaryCmd) -> {
                val offset = commandApdu.sliceArray(2..3).toHexString().toInt(16)
                val length = commandApdu.sliceArray(4..4).toHexString().toInt(16)

                val fullResponse = ByteArray(ndefLength.size + ndefBytes.size)
                System.arraycopy(ndefLength, 0, fullResponse, 0, ndefLength.size)
                System.arraycopy(ndefBytes, 0, fullResponse, ndefLength.size, ndefBytes.size)

                Log.i(logTag, "readBinaryCmd triggered. Full data: " + fullResponse.toHexString())
                Log.i(logTag, "readBinary - OFFSET: $offset - LEN: $length")

                val slicedResponse = fullResponse.sliceArray(offset until fullResponse.size)

                val actualLength = if (slicedResponse.size <= length) slicedResponse.size else length
                val response = ByteArray(actualLength + successResponse.size)

                System.arraycopy(slicedResponse, 0, response, 0, actualLength)
                System.arraycopy(successResponse, 0, response, actualLength, successResponse.size)

                Log.i(logTag, "readBinaryCmd triggered. Response: " + response.toHexString())

                isCapabilityContainerRead = false
                return response
            }
            else -> {
                Log.i(logTag, "No matching APDU command. Returning error response.")
                errorResponse
            }
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.i(logTag, "onDeactivated: $reason")
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    private fun createUriRecord(uri: String): NdefRecord {
        return NdefRecord.createUri(uri)
    }

    private fun Int.toByteArray(size: Int): ByteArray {
        return ByteArray(size) { i -> (this shr (8 * (size - 1 - i)) and 0xFF).toByte() }
    }
}