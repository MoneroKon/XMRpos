// LoginViewModel.kt
package org.monerokon.xmrpos.ui.payment.login

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.AuthRepository
import org.monerokon.xmrpos.data.repository.BackendRepository
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.ExchangeRateRepository
import org.monerokon.xmrpos.shared.DataResult
import org.monerokon.xmrpos.ui.PaymentCheckout
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val logTag = "LoginViewModel"

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    var instanceUrl by mutableStateOf("")

    var vendorID by mutableStateOf("")

    var username by mutableStateOf("")

    var password by mutableStateOf("")

    var errorMessage by mutableStateOf("")

    var inProgress by mutableStateOf(false)

    init {

    }

    fun updateInstanceUrl(instanceUrl: String) {
        this.instanceUrl = instanceUrl
    }

    fun updateVendorID(vendorID: String) {
        this.vendorID = vendorID
    }

    fun updateUsername(username: String) {
        this.username = username
    }

    fun updatePassword(password: String) {
        this.password = password
    }

    fun resetErrorMessage() {
        errorMessage = ""
    }

    fun loginPressed() {
        errorMessage = ""
        if (instanceUrl == "") {
            errorMessage = "Instance URL is required"
            return
        }
        if (vendorID == "") {
            errorMessage = "Vendor ID is required"
            return
        }
        try {
            val id = vendorID.toInt()
        } catch (
            e: NumberFormatException
        ) {
            errorMessage = "Vendor ID must be a number"
            return
        }
        if (username == "") {
            errorMessage = "Username is required"
            return
        }
        if (password == "") {
            errorMessage = "Password is required"
            return
        }

        viewModelScope.launch {
            login()
        }
    }

    private suspend fun login() {
        inProgress = true
        val resp = authRepository.login(instanceUrl = instanceUrl, vendorID = vendorID.toInt(), username = username, password = password)
        inProgress = false
        resp.fold(
            onSuccess = {

            },
            onFailure = {
                errorMessage = it.message ?: "Unknown error"
            }
        )
    }

}