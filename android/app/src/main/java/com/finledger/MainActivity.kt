package com.finledger

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.model.UserPreferences
import com.finledger.ui.navigation.FinLedgerBottomBar
import com.finledger.ui.navigation.FinLedgerNavHost
import com.finledger.ui.navigation.PrimaryBottomNavItems
import com.finledger.ui.screens.security.AppLockManager
import com.finledger.ui.screens.security.BiometricHelper
import com.finledger.ui.screens.security.OnboardingFlow
import com.finledger.ui.screens.security.PinUnlockOverlay
import com.finledger.ui.screens.settings.shouldUseDarkTheme
import com.finledger.ui.theme.FinLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

        val initialPrefs = runBlocking { userPreferencesRepository.preferences.first() }
        appLockManager.lockIfEnabled(initialPrefs.pinEnabled)

        setContent {
            val prefs by userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = initialPrefs)
            val pinHash by userPreferencesRepository.pinHash
                .collectAsStateWithLifecycle(initialValue = null)
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()
            var biometricPrompted by remember { mutableStateOf(false) }

            LaunchedEffect(isLocked, prefs.biometricEnabled) {
                if (isLocked && prefs.biometricEnabled && !biometricPrompted && BiometricHelper.isAvailable(this@MainActivity)) {
                    biometricPrompted = true
                    BiometricHelper.authenticate(
                        activity = this@MainActivity,
                        onSuccess = { appLockManager.unlock() },
                        onError = { /* User falls back to PIN */ },
                    )
                }
            }

            FinLedgerTheme(darkTheme = prefs.themeMode.shouldUseDarkTheme()) {
                when {
                    isLocked && prefs.pinEnabled -> {
                        PinUnlockOverlay(
                            storedHash = pinHash,
                            biometricEnabled = prefs.biometricEnabled,
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

@Composable
fun FinLedgerAppShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = PrimaryBottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
            modifier = Modifier.padding(innerPadding),
        )
    }
}
