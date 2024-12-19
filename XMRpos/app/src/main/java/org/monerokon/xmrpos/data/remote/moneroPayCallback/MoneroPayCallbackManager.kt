package org.monerokon.xmrpos.data.remote.moneroPayCallback

import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallback

class MoneroPayCallbackManager(private val port: Int) {
    private var callbackServer: MoneroPayCallbackServer? = null

    fun startListening(onPaymentReceived: (PaymentCallback, Double, String) -> Unit) {
        if (callbackServer == null) {
            callbackServer = MoneroPayCallbackServer(port, onPaymentReceived)
        }
    }

    fun stopListening() {
        MoneroPayCallbackServer.stopServer(callbackServer)
        callbackServer = null
    }
}
