package org.monerokon.xmrpos.ui.settings.moneropay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    var requirePinCodeOnAppStart by mutableStateOf(false)
    var requirePinCodeOpenSettings by mutableStateOf(false)
    var pinCodeOnAppStart by mutableStateOf("")
    var pinCodeOpenSettings by mutableStateOf("")

    init {
        viewModelScope.launch {
            dataStoreRepository.getRequirePinCodeOnAppStart().collect { storedRequirePinCodeOnAppStart ->
                requirePinCodeOnAppStart = storedRequirePinCodeOnAppStart
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getRequirePinCodeOnOpenSettings().collect { storedRequirePinCodeOpenSettings ->
                requirePinCodeOpenSettings = storedRequirePinCodeOpenSettings
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPinCodeOnAppStart().collect { storedPinCodeOnAppStart ->
                pinCodeOnAppStart = storedPinCodeOnAppStart
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getPinCodeOpenSettings().collect { storedPinCodeOpenSettings ->
                pinCodeOpenSettings = storedPinCodeOpenSettings
            }
        }
    }

    fun updateRequirePinCodeOnAppStart(newRequirePinCodeOnAppStart: Boolean) {
        requirePinCodeOnAppStart = newRequirePinCodeOnAppStart
        viewModelScope.launch {
            dataStoreRepository.saveRequirePinCodeOnAppStart(newRequirePinCodeOnAppStart)
        }
    }

    fun updateRequirePinCodeOpenSettings(newRequirePinCodeOpenSettings: Boolean) {
        requirePinCodeOpenSettings = newRequirePinCodeOpenSettings
        viewModelScope.launch {
            dataStoreRepository.saveRequirePinCodeOnOpenSettings(newRequirePinCodeOpenSettings)
        }
    }

    fun updatePinCodeOnAppStart(newPinCodeOnAppStart: String) {
        if (newPinCodeOnAppStart.length > 16) {
            return
        }
        if (newPinCodeOnAppStart.isNotEmpty() && !newPinCodeOnAppStart.matches(Regex("^[0-9]*\$"))) {
            return
        }
        pinCodeOnAppStart = newPinCodeOnAppStart
        viewModelScope.launch {
            dataStoreRepository.savePinCodeOnAppStart(newPinCodeOnAppStart)
        }
    }

    fun updatePinCodeOpenSettings(newPinCodeOpenSettings: String) {
        if (newPinCodeOpenSettings.length > 16) {
            return
        }
        if (newPinCodeOpenSettings.isNotEmpty() && !newPinCodeOpenSettings.matches(Regex("^[0-9]*\$"))) {
            return
        }
        pinCodeOpenSettings = newPinCodeOpenSettings
        viewModelScope.launch {
            dataStoreRepository.savePinCodeOpenSettings(newPinCodeOpenSettings)
        }
    }
}


