package org.monerokon.xmrpos

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.local.usb.UsbPermissionHandler
import org.monerokon.xmrpos.data.repository.AuthRepository
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.ui.NavGraphRoot
import org.monerokon.xmrpos.ui.Login
import org.monerokon.xmrpos.ui.PaymentEntry
import org.monerokon.xmrpos.ui.security.PinProtectScreenRoot
import org.monerokon.xmrpos.ui.theme.XMRposTheme
import javax.inject.Inject

sealed class InitialScreenState {
    object Loading : InitialScreenState()
    object RequiresLogin : InitialScreenState()
    data class RequiresPin(val pinCode: String) : InitialScreenState() // Include PIN if needed
    object MainApp : InitialScreenState()
}
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var dataStoreRepository: DataStoreRepository

    companion object {
        private const val ACTION_USB_PERMISSION = "org.monerokon.xmrpos.USB_PERMISSION"
    }

    private val usbPermissionHandler = UsbPermissionHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splash.setKeepOnScreenCondition { true };

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val filter = IntentFilter(ACTION_USB_PERMISSION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionHandler.usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // For older API levels, use the standard registerReceiver method
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbPermissionHandler.usbReceiver, filter)
        }

        setContent {
            XMRposTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AuthAndPinGate(
                        navController,
                        authRepository = authRepository,
                        dataStoreRepository = dataStoreRepository,
                        onStateDetermined = {
                           splash.setKeepOnScreenCondition { false }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun AuthAndPinGate(
        navController: NavHostController,
        authRepository: AuthRepository,
        dataStoreRepository: DataStoreRepository,
        onStateDetermined: () -> Unit
    ) {
        // Stage 1: Initial state determination for splash screen & cold start
        var initialScreenState by remember { mutableStateOf<InitialScreenState>(InitialScreenState.Loading) }
        val currentOnStateDetermined by rememberUpdatedState(onStateDetermined)

        LaunchedEffect(key1 = Unit) {
            val isLoggedInInitial = authRepository.isLoggedIn().first()
            val determinedState = if (isLoggedInInitial) {
                // User IS logged in on cold start, check for PIN requirement
                val requirePinOnStart = dataStoreRepository.getRequirePinCodeOnAppStart().first()
                if (requirePinOnStart) {
                    val pinCode = dataStoreRepository.getPinCodeOnAppStart().first()
                    if (pinCode.isNotBlank()) {
                        InitialScreenState.RequiresPin(pinCode)
                    } else {
                        InitialScreenState.MainApp // PIN required but not set, default to MainApp
                    }
                } else {
                    InitialScreenState.MainApp // Logged in, no PIN on start required
                }
            } else {
                InitialScreenState.RequiresLogin // Not logged in on cold start
            }
            initialScreenState = determinedState
            currentOnStateDetermined()
        }

        // Stage 2: Continuous observation of login state for ongoing session management
        val isLoggedInRealtime by authRepository.isLoggedIn()
            .collectAsStateWithLifecycle(initialValue = null) // Start null to await first real emission

        // Stage 3: Navigation logic based on real-time login state changes AFTER initial load
        LaunchedEffect(isLoggedInRealtime, initialScreenState) {
            if (initialScreenState is InitialScreenState.Loading || isLoggedInRealtime == null) {
                // Still waiting for initial determination or first real-time auth status.
                return@LaunchedEffect
            }

            if (isLoggedInRealtime == false) {
                // User is NOT logged in (or became unauthenticated).
                // Force navigation to Login screen if not already there.
                if (navController.currentDestination?.route != Login.toString()) {
                    navController.navigate(Login) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            } else { // isLoggedInRealtime == true
                // User IS logged in.
                // If the current screen is Login, it means the user just actively logged in.
                // Since PIN is only for cold starts, navigate directly to MainApp (PaymentEntry).
                // This assumes LoginScreen itself doesn't navigate, or if it does, this acts as a backup.
                if (navController.currentDestination?.route == Login::class.qualifiedName) {
                    navController.navigate(PaymentEntry) { // Type-safe navigation
                        popUpTo(Login::class.qualifiedName!!) { inclusive = true } // Pop Login screen
                        launchSingleTop = true
                    }
                }
                // If already on MainApp or PinProtect (from cold start), do nothing, state is fine.
            }
        }

        // Stage 4: Rendering logic based on the most current understanding of what to show
        // This primarily uses `initialScreenState` for the first paint after splash.
        // The LaunchedEffect above handles *changes* from that initial state.
        when (val currentDisplayState = initialScreenState) {
            InitialScreenState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Splash screen covers this
                }
            }
            InitialScreenState.RequiresLogin -> {
                // If isLoggedInRealtime becomes true, LaunchedEffect will navigate away from here.
                NavGraphRoot(navController = navController, startDestination = Login)
            }
            is InitialScreenState.RequiresPin -> {
                // This is ONLY for cold starts where user was already logged in AND PIN was required.
                // If isLoggedInRealtime becomes false, LaunchedEffect will navigate to Login.
                PinProtectScreenRoot(
                    pinCode = currentDisplayState.pinCode,
                    protectedScreen = { NavGraphRoot(navController = navController, startDestination = PaymentEntry) }
                )
            }
            InitialScreenState.MainApp -> {
                // If isLoggedInRealtime becomes false, LaunchedEffect will navigate to Login.
                NavGraphRoot(navController = navController, startDestination = PaymentEntry)
            }
        }
    }
}