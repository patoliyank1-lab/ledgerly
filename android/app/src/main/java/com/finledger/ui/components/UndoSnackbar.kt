package com.finledger.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Default display duration (ms) before a pending Undo snackbar auto-dismisses.
 */
const val DEFAULT_UNDO_TIMEOUT_MS = 5_000L

/**
 * Reversible delete notification payload supporting asynchronous restore callbacks.
 */
data class UndoEvent(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val actionLabel: String = "UNDO",
    val onUndo: suspend () -> Unit,
)

/**
 * Coordinates app-wide or screen-local delete-with-undo events cleanly across navigation boundaries.
 */
class UndoCoordinator {
    private val _currentEvent = MutableStateFlow<UndoEvent?>(null)
    val currentEvent: StateFlow<UndoEvent?> = _currentEvent.asStateFlow()

    fun showUndo(
        message: String,
        actionLabel: String = "UNDO",
        onUndo: suspend () -> Unit,
    ) {
        _currentEvent.value = UndoEvent(
            message = message,
            actionLabel = actionLabel,
            onUndo = onUndo,
        )
    }

    fun showDeleted(
        itemLabel: String,
        onUndo: suspend () -> Unit,
    ) {
        val trimmed = itemLabel.trim()
        val msg = if (trimmed.isEmpty()) "Deleted" else "Deleted $trimmed"
        showUndo(message = msg, onUndo = onUndo)
    }

    suspend fun performUndo(event: UndoEvent) {
        clearIfMatches(event.id)
        event.onUndo()
    }

    fun clearIfMatches(eventId: String) {
        if (_currentEvent.value?.id == eventId) {
            _currentEvent.value = null
        }
    }

    fun dismiss() {
        _currentEvent.value = null
    }
}

val LocalUndoCoordinator = staticCompositionLocalOf<UndoCoordinator?> { null }

/**
 * Observes [coordinator] and displays a bounded-timeout Material 3 Undo snackbar on [snackbarHostState].
 */
@Composable
fun UndoSnackbarEffect(
    coordinator: UndoCoordinator,
    snackbarHostState: SnackbarHostState,
    timeoutMs: Long = DEFAULT_UNDO_TIMEOUT_MS,
) {
    val event by coordinator.currentEvent.collectAsStateWithLifecycle()

    LaunchedEffect(event?.id) {
        val current = event ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                snackbarHostState.showSnackbar(
                    message = current.message,
                    actionLabel = current.actionLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == null) {
                snackbarHostState.currentSnackbarData?.dismiss()
            } else if (result == SnackbarResult.ActionPerformed) {
                coordinator.performUndo(current)
            }
        } finally {
            coordinator.clearIfMatches(current.id)
        }
    }
}

