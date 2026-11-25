package com.finledger.ui.screens.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.finledger.data.local.entity.AccountEntity
import com.finledger.data.local.entity.AccountType
import com.finledger.data.repository.AccountRepository
import com.finledger.data.repository.UserPreferencesRepository
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.components.AmountTextField
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

object ErrorLogger {
    fun logLocal(context: Context, tag: String, throwable: Throwable) {
        try {
            val file = File(context.filesDir, "finledger_errors.log")
            file.appendText("${System.currentTimeMillis()} [$tag] ${throwable.stackTraceToString()}\n")
        } catch (_: Exception) {
        }
    }
}

object BiometricHelper {
    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinLedger")
            .setSubtitle("Use biometrics to view your personal financial ledger")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }
}

@Singleton
class AppLockManager @Inject constructor() {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    fun lockIfEnabled(pinEnabled: Boolean) {
        if (pinEnabled) _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
    }
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: AccountRepository,
    private val databaseSeeder: com.finledger.data.local.DatabaseSeeder,
) : ViewModel() {

    val securityState = combine(
        userPreferencesRepository.preferences,
        userPreferencesRepository.pinHash,
    ) { prefs, hash -> prefs to hash }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setPin(rawPin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setPin(hashPin(rawPin))
            onDone()
        }
    }

    fun disablePin(onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setPin(null)
            userPreferencesRepository.setBiometricEnabled(false)
            onDone()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setBiometricEnabled(enabled) }
    }

    fun verifyPin(rawPin: String, storedHash: String?): Boolean {
        return storedHash != null && hashPin(rawPin) == storedHash
    }

    fun completeOnboarding(
        userName: String,
        currency: String,
        firstAccountName: String,
        openingBalancePaise: Long,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if (userName.isNotBlank()) userPreferencesRepository.setUserName(userName)
            userPreferencesRepository.setPrimaryCurrency(currency)
            if (firstAccountName.isNotBlank()) {
                accountRepository.saveAccount(
                    AccountEntity(
                        name = firstAccountName.trim(),
                        type = AccountType.SAVINGS,
                        currency = currency,
                        openingBalance = openingBalancePaise,
                    )
                )
            } else {
                databaseSeeder.seedInitialCashWallet(currency)
            }
            userPreferencesRepository.setCashWalletSeeded(true)
            userPreferencesRepository.setOnboardingCompleted(true)
            onDone()
        }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

@Composable
fun PinLockSettingsScreen(
    onBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    val pair = viewModel.securityState.collectAsStateWithLifecycle().value
    val prefs = pair?.first
    val storedHash = pair?.second
    val context = LocalContext.current
    var currentPinInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Security & App Lock", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = if (prefs?.pinEnabled == true) "PIN Lock is Enabled" else "Set a 4-6 digit PIN to protect your offline ledger.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (prefs?.pinEnabled == true) {
                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = { currentPinInput = it.filter(Char::isDigit).take(6); error = null; statusMessage = null },
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it.filter(Char::isDigit).take(6); error = null; statusMessage = null },
                label = { Text("New PIN (4–6 digits)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = confirmPinInput,
                onValueChange = { confirmPinInput = it.filter(Char::isDigit).take(6); error = null; statusMessage = null },
                label = { Text("Confirm New PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let { Text(it, color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall) }
            statusMessage?.let { Text(it, color = FinLedgerTheme.colors.positive, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = {
                    if (prefs?.pinEnabled == true && !viewModel.verifyPin(currentPinInput, storedHash)) {
                        error = "Current PIN is incorrect"
                        return@Button
                    }
                    if (pinInput.length < 4) {
                        error = "PIN must be at least 4 digits"
                        return@Button
                    }
                    if (pinInput != confirmPinInput) {
                        error = "PIN entries do not match"
                        return@Button
                    }
                    viewModel.setPin(pinInput) {
                        currentPinInput = ""
                        pinInput = ""
                        confirmPinInput = ""
                        statusMessage = "PIN saved successfully"
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (prefs?.pinEnabled == true) "Update PIN" else "Enable PIN Lock") }

            if (prefs?.pinEnabled == true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Unlock with Biometrics", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = prefs.biometricEnabled,
                        enabled = BiometricHelper.isAvailable(context),
                        onCheckedChange = viewModel::setBiometricEnabled,
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (!viewModel.verifyPin(currentPinInput, storedHash)) {
                            error = "Enter your current PIN to disable PIN lock"
                            return@OutlinedButton
                        }
                        viewModel.disablePin {
                            currentPinInput = ""
                            pinInput = ""
                            confirmPinInput = ""
                            statusMessage = "PIN lock disabled"
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Disable PIN Lock") }
            }
        }
    }
}

@Composable
fun PinUnlockOverlay(
    storedHash: String?,
    biometricEnabled: Boolean,
    onRequestBiometric: () -> Unit = {},
    onUnlocked: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("FinLedger Locked", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text("Enter your PIN to continue", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
            Spacer(modifier = Modifier.height(Spacing.lg))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6); error = false },
                label = { Text("PIN") },
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text("Incorrect PIN", color = FinLedgerTheme.colors.negative, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            Button(
                onClick = {
                    if (viewModel.verifyPin(pin, storedHash)) onUnlocked() else error = true
                },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Unlock") }
            if (biometricEnabled && BiometricHelper.isAvailable(context)) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = onRequestBiometric,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Unlock with Biometrics") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    var step by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("Personal") }
    var currency by remember { mutableStateOf("INR") }
    var accountName by remember { mutableStateOf("Main Bank Account") }
    var openingText by remember { mutableStateOf("") }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                0 -> {
                    Text("FinLedger", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Your offline personal finance ledger. Track accounts, shared expenses, loans, investments, and net worth in one private place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FinLedgerTheme.colors.inkMuted80,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Button(
                        onClick = { step = 1 },
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                    ) { Text("Get Started") }
                }
                1 -> {
                    Text("Profile & Currency", style = MaterialTheme.typography.displaySmall)
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text("Primary Currency", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        listOf("INR", "USD", "EUR", "GBP", "AED").forEach { code ->
                            FilterChip(selected = currency == code, onClick = { currency = code }, label = { Text(code) })
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Button(
                        onClick = { step = 2 },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Continue") }
                }
                else -> {
                    Text("Add Your First Bank Account", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text("A Cash Wallet is already ready. Add your primary bank account or skip.", style = MaterialTheme.typography.bodySmall, color = FinLedgerTheme.colors.inkMuted48)
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Account Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    AmountTextField(
                        value = openingText,
                        onValueChange = { openingText = it },
                        label = "Opening Balance",
                        currency = currency,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    Button(
                        onClick = {
                            viewModel.completeOnboarding(
                                userName = userName,
                                currency = currency,
                                firstAccountName = accountName,
                                openingBalancePaise = MoneyFormatter.parseToPaise(openingText) ?: 0L,
                                onDone = onComplete,
                            )
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Finish Setup") }
                }
            }
        }
    }
}

