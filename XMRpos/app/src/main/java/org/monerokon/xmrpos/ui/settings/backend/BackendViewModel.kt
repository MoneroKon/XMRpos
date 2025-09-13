package org.monerokon.xmrpos.ui.settings.backend

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.AuthRepository
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.BackendRepository
import org.monerokon.xmrpos.shared.DataResult
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class BackendViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val backendRepository: BackendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val logTag = "BackendViewModel"

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    val confOptions = listOf("0-conf", "1-conf", "10-conf")

    var instanceUrl: String by mutableStateOf("")

    var requestInterval: String by mutableStateOf("5")

    var conf: String by mutableStateOf("")

    var healthStatus by mutableStateOf("")

    init {
        viewModelScope.launch {
            dataStoreRepository.getBackendInstanceUrl().collect { storedInstanceUrl ->
                Log.i(logTag, "storedInstanceUrl: $storedInstanceUrl")
                instanceUrl = storedInstanceUrl
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getBackendConfValue().collect { storedConfValue ->
                Log.i(logTag, "storedConfValue: $storedConfValue")
                conf = storedConfValue
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getBackendRequestInterval().collect { storedRequestInterval ->
                Log.i(logTag, "storedRequestInterval: $storedRequestInterval")
                requestInterval = storedRequestInterval.toString()
            }
        }
    }

    fun updateRequestInterval(newRequestInterval: String) {
        if (newRequestInterval.isEmpty()) {
            requestInterval = ""
            viewModelScope.launch {
                dataStoreRepository.saveBackendRequestInterval(5)
            }
            return
        }
        if (newRequestInterval.all { it.isDigit() }) {
            requestInterval = newRequestInterval
            viewModelScope.launch {
                dataStoreRepository.saveBackendRequestInterval(newRequestInterval.toInt())
            }
        }
    }

    fun updateConf(newConf: String) {
        conf = newConf
        viewModelScope.launch {
            dataStoreRepository.saveBackendConfValue(newConf)
        }
    }

    fun fetchBackendHealth() {
        viewModelScope.launch {
            val response = backendRepository.health()
            if (response is DataResult.Success) {
                Log.i(logTag, "Backend health: ${response.data}")
                healthStatus = response.data.toString()
            } else if (response is DataResult.Failure) {
                Log.e(logTag, "Backend health: ${response.message}")
                healthStatus = response.message
            }
        }
    }

    fun resetHealthStatus() {
        healthStatus = ""
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
