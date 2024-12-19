package org.monerokon.xmrpos.data.remote.moneroPayCallback

import android.os.Handler
import android.os.Looper
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import kotlinx.serialization.json.Json
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallback

class MoneroPayCallbackServer(
    port: Int,
    private val onPaymentReceived: (PaymentCallback, Double, String) -> Unit
) : NanoHTTPD(port) {

    private val logTag = "MoneroPayCallbackServer"

    init {
        start(SOCKET_READ_TIMEOUT, false)
    }

    override fun serve(session: IHTTPSession): Response {
        return if (session.method == Method.POST) {
            val bodyData = mutableMapOf<String, String>()
            session.parseBody(bodyData)
            val callbackData = bodyData["postData"] ?: return newFixedLengthResponse("No callback data")

            // parse the fiatValue from the request parameters
            val fiatValue = session.parms["fiatValue"]?.toDoubleOrNull() ?: 0.0

            // parse the callbackUUID from the request parameters
            val callbackUUID = session.parms["callbackUUID"]?.toString() ?: ""

            processPaymentEvent(callbackData, fiatValue, callbackUUID)
            return newFixedLengthResponse("Callback processed successfully")
        } else {
            return newFixedLengthResponse("Invalid request method")
        }
    }

    private fun processPaymentEvent(data: String, fiatValue: Double, callbackUUID: String) {
        try {

            var paymentCallback = Json.decodeFromString<PaymentCallback>(data)

            Handler(Looper.getMainLooper()).post {
                onPaymentReceived(paymentCallback, fiatValue, callbackUUID)
            }
        } catch (e: Exception) {
            Log.i(logTag, "Failed to process callback: ${e.message}")
        }
    }

    companion object {
        fun stopServer(server: MoneroPayCallbackServer?) {
            server?.stop()
        }
    }
}
