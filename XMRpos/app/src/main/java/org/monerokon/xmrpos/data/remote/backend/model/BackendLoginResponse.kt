package org.monerokon.xmrpos.data.remote.backend.model

data class BackendLoginResponse(
    val access_token: String,
    val refresh_token: String,
)