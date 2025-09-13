package org.monerokon.xmrpos.data.remote.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResponse(
    val access_token: String,
    val refresh_token: String,
)
