// CompanyInformationViewModel.kt
package org.monerokon.xmrpos.ui.settings.companyinformation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.StorageRepository
import org.monerokon.xmrpos.ui.Settings
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CompanyInformationViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val storageRepository: StorageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    var companyLogo: File? by mutableStateOf(null)

    var companyName: String by mutableStateOf("")

    var contactInformation: String by mutableStateOf("")

    var receiptFooter: String by mutableStateOf("")

    // Load data from DataStore when ViewModel is initialized
    init {
        viewModelScope.launch {
            dataStoreRepository.getCompanyName().collect { storedCompanyName ->
                companyName = storedCompanyName
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getContactInformation().collect { storedContactInformation ->
                contactInformation = storedContactInformation
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getReceiptFooter().collect { storedReceiptFooter ->
                receiptFooter = storedReceiptFooter
            }
        }
        companyLogo = storageRepository.readImage("logo.png")
    }

    fun updateCompanyName(newCompanyName: String) {
        companyName = newCompanyName
        viewModelScope.launch {
            dataStoreRepository.saveCompanyName(newCompanyName)
        }
    }

    fun updateContactInformation(newContactInformation: String) {
        contactInformation = newContactInformation
        viewModelScope.launch {
            dataStoreRepository.saveContactInformation(newContactInformation)
        }
    }

    fun updateReceiptFooter(newReceiptFooter: String) {
        receiptFooter = newReceiptFooter
        viewModelScope.launch {
            dataStoreRepository.saveReceiptFooter(newReceiptFooter)
        }
    }

    fun saveLogo(uri: Uri) {
        companyLogo = storageRepository.saveImage(uri, "logo.png")
    }

    fun deleteLogo() {
        companyLogo?.let {
            storageRepository.deleteImage("logo.png")
            companyLogo = null
        }
    }

}


