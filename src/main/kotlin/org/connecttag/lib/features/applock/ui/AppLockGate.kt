package org.connecttag.lib.features.applock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.connecttag.lib.features.applock.AppLockGateMode
import org.connecttag.lib.features.applock.AppLockGateResolver
import org.connecttag.lib.features.applock.AppLockManager
import org.connecttag.lib.features.applock.PinPolicyConfig

/**
 * Stateless fail-closed Composable gate that routes between [loadingContent], [lockedContent],
 * [decoyContent], and [content] according to [gateMode].
 *
 * Ensures protected [content] is never rendered while [gateMode] is [AppLockGateMode.LOADING],
 * [AppLockGateMode.LOCKED], or [AppLockGateMode.DECOY].
 */
@Composable
public fun AppLockGate(
    gateMode: AppLockGateMode,
    modifier: Modifier = Modifier,
    loadFailed: Boolean = false,
    interceptBackWhenLocked: Boolean = true,
    onLockedBackPressed: () -> Unit = {},
    strings: PinLockStrings = PinLockStrings(),
    loadingContent: @Composable (loadFailed: Boolean) -> Unit = { failed ->
        DefaultAppLockLoadingScreen(
            loadFailed = failed,
            strings = strings,
        )
    },
    lockedContent: @Composable () -> Unit,
    decoyContent: @Composable () -> Unit = lockedContent,
    content: @Composable () -> Unit,
) {
    BackHandler(
        enabled = interceptBackWhenLocked && (
            gateMode == AppLockGateMode.LOCKED ||
                gateMode == AppLockGateMode.DECOY ||
                gateMode == AppLockGateMode.LOADING
            ),
        onBack = onLockedBackPressed,
    )

    Box(modifier = modifier.fillMaxSize()) {
        when (gateMode) {
            AppLockGateMode.UNLOCKED -> content()
            AppLockGateMode.LOADING -> loadingContent(loadFailed)
            AppLockGateMode.LOCKED -> lockedContent()
            AppLockGateMode.DECOY -> decoyContent()
        }
    }
}

/**
 * Stateful fail-closed Composable gate backed by [AppLockManager] that observes [AppLockManager.snapshotFlow],
 * automatically renders [PinLockScreen] (or a custom [lockedContent] disguise screen) when locked,
 * and routes to [decoyContent] when unlocked via a Duress/Decoy PIN.
 */
@Composable
public fun AppLockGate(
    manager: AppLockManager,
    modifier: Modifier = Modifier,
    isStateLoaded: Boolean = true,
    loadFailed: Boolean = false,
    pinLength: Int = PinPolicyConfig.DEFAULT_MIN_PIN_LENGTH,
    autoSubmit: Boolean = true,
    showBiometricButton: Boolean = false,
    onBiometricRequest: (() -> Unit)? = null,
    interceptBackWhenLocked: Boolean = true,
    onLockedBackPressed: () -> Unit = {},
    onMasterUnlocked: () -> Unit = {},
    onDuressUnlocked: () -> Unit = {},
    colors: PinLockColors = PinLockDefaults.colors(),
    strings: PinLockStrings = PinLockStrings(),
    loadingContent: @Composable (loadFailed: Boolean) -> Unit = { failed ->
        DefaultAppLockLoadingScreen(
            loadFailed = failed,
            strings = strings,
        )
    },
    lockedContent: (@Composable () -> Unit)? = null,
    decoyContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val snapshot by manager.snapshotFlow.collectAsState()
    val gateMode = if (!isStateLoaded || loadFailed) {
        AppLockGateMode.LOADING
    } else {
        AppLockGateResolver.resolve(
            policy = snapshot.policy,
            isAuthenticated = !snapshot.locked,
            loadFailed = false,
            tamperDetected = snapshot.tamperDetected,
        )
    }

    AppLockGate(
        gateMode = gateMode,
        modifier = modifier,
        loadFailed = loadFailed,
        interceptBackWhenLocked = interceptBackWhenLocked,
        onLockedBackPressed = onLockedBackPressed,
        strings = strings,
        loadingContent = loadingContent,
        lockedContent = {
            if (lockedContent != null) {
                lockedContent()
            } else {
                PinLockScreen(
                    manager = manager,
                    pinLength = pinLength,
                    autoSubmit = autoSubmit,
                    showBiometricButton = showBiometricButton,
                    onBiometricRequest = onBiometricRequest,
                    onAuthenticated = onMasterUnlocked,
                    onDuressAuthenticated = onDuressUnlocked,
                    colors = colors,
                    strings = strings,
                )
            }
        },
        decoyContent = decoyContent ?: content,
        content = content,
    )
}

@Composable
private fun DefaultAppLockLoadingScreen(
    loadFailed: Boolean,
    strings: PinLockStrings,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LOADING_PADDING_DP.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!loadFailed) {
                CircularProgressIndicator()
            }
            Text(
                text = if (loadFailed) {
                    strings.loadFailedSecurityLabel
                } else {
                    strings.loadingSecurityLabel
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = LOADING_TEXT_TOP_PADDING_DP.dp),
            )
        }
    }
}

private const val LOADING_PADDING_DP: Int = 24
private const val LOADING_TEXT_TOP_PADDING_DP: Int = 12
