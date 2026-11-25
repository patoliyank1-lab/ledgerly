package com.finledger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.UserPreferences
import com.finledger.ui.components.LocalUndoCoordinator
import com.finledger.ui.components.UndoCoordinator
import com.finledger.ui.components.UndoSnackbarEffect
import com.finledger.ui.navigation.FinLedgerBottomBar
import com.finledger.ui.navigation.FinLedgerNavHost
import com.finledger.ui.navigation.PrimaryBottomNavItems
import com.finledger.ui.screens.security.AppLockManager
import com.finledger.ui.screens.security.BiometricHelper
import com.finledger.ui.screens.security.LocalSuppressAppLock
import com.finledger.ui.screens.security.OnboardingFlow
import com.finledger.ui.screens.security.PinUnlockOverlay
import com.finledger.ui.screens.settings.shouldUseDarkTheme
import com.finledger.ui.theme.FinLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var appLockManager: AppLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefsState by remember {
                userPreferencesRepository.preferences.map<UserPreferences, UserPreferences?> { it }
            }.collectAsStateWithLifecycle(initialValue = null)
            val pinHash by userPreferencesRepository.pinHash
                .collectAsStateWithLifecycle(initialValue = null)
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()
            var biometricPrompted by remember { mutableStateOf(false) }
            var initialLockChecked by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(prefsState) {
                val p = prefsState
                if (p != null && !initialLockChecked) {
                    appLockManager.lockIfEnabled(p.pinEnabled)
                    initialLockChecked = true
                }
            }

            DisposableEffect(lifecycleOwner, prefsState?.pinEnabled, prefsState?.lockTimeoutSeconds) {
                val observer = LifecycleEventObserver { _, event ->
                    val currentPrefs = prefsState
                    if (event == Lifecycle.Event.ON_STOP && !isChangingConfigurations) {
                        appLockManager.onAppBackgrounded(pinEnabled = currentPrefs?.pinEnabled == true)
                    } else if (event == Lifecycle.Event.ON_START && initialLockChecked && currentPrefs != null) {
                        val wasLocked = appLockManager.isLocked.value
                        appLockManager.onAppForegrounded(
                            pinEnabled = currentPrefs.pinEnabled,
                            timeoutSeconds = currentPrefs.lockTimeoutSeconds,
                        )
                        if (!wasLocked && appLockManager.isLocked.value) {
                            biometricPrompted = false
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(isLocked) {
                if (!isLocked) {
                    biometricPrompted = false
                }
            }

            val prefs = prefsState
            if (prefs == null) {
                FinLedgerTheme {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxSize(),
                    ) { Box(modifier = Modifier.fillMaxSize()) }
                }
                return@setContent
            }

            val triggerBiometric: () -> Unit = {
                if (prefs.biometricEnabled && BiometricHelper.isAvailable(this@MainActivity)) {
                    appLockManager.suppressNextBackgroundLock()
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { appLockManager.unlock() },
                        onError = { /* User falls back to PIN */ },
                    )
                }
            }

            LaunchedEffect(isLocked, prefs.biometricEnabled) {
                if (isLocked && prefs.biometricEnabled && !biometricPrompted && BiometricHelper.isAvailable(this@MainActivity)) {
                    biometricPrompted = true
                    triggerBiometric()
                }
            }

            CompositionLocalProvider(
                LocalSuppressAppLock provides { appLockManager.suppressNextBackgroundLock() }
            ) {
                FinLedgerTheme(darkTheme = prefs.themeMode.shouldUseDarkTheme()) {
                    when {
                        isLocked && prefs.pinEnabled -> {
                            PinUnlockOverlay(
                                storedHash = pinHash,
                                biometricEnabled = prefs.biometricEnabled,
                                onRequestBiometric = triggerBiometric,
                                onUnlocked = { appLockManager.unlock() },
                            )
                        }
                        !prefs.onboardingCompleted -> {
                            OnboardingFlow(onComplete = {})
                        }
                        else -> {
                            FinLedgerAppShell()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinLedgerAppShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = PrimaryBottomNavItems.any { it.route == currentRoute }
    val undoCoordinator = remember { UndoCoordinator() }
    val snackbarHostState = remember { SnackbarHostState() }

    UndoSnackbarEffect(
        coordinator = undoCoordinator,
        snackbarHostState = snackbarHostState,
    )

    CompositionLocalProvider(LocalUndoCoordinator provides undoCoordinator) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = if (!showBottomBar) Modifier.navigationBarsPadding() else Modifier,
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    FinLedgerBottomBar(
                        currentRoute = currentRoute,
                        onSelectRoute = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            FinLedgerNavHost(
                navController = navController,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}
