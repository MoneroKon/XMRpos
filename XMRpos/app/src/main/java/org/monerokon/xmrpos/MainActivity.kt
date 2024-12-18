package org.monerokon.xmrpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.ui.NavGraphRoot
import org.monerokon.xmrpos.ui.security.PinProtectScreenRoot
import org.monerokon.xmrpos.ui.theme.XMRposTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var dataStoreRepository: DataStoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        lifecycleScope.launch {
            val requirePinCodeOnAppStart = dataStoreRepository.getRequirePinCodeOnAppStart().first()
            val pinCodeOnAppStart = dataStoreRepository.getPinCodeOnAppStart().first()
            setContent {
                XMRposTheme {
                    if (requirePinCodeOnAppStart == true) {
                        PinProtectScreenRoot(protectedScreen = {NavGraphRoot()}, pinCode = pinCodeOnAppStart)
                    } else {
                        NavGraphRoot()
                    }
                }
            }
        }
    }
}