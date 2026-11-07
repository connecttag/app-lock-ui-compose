package org.connecttag.lib.features.applock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import org.connecttag.lib.features.applock.AppLockBiometricAuthenticator
import org.connecttag.lib.features.applock.AppLockBiometricPromptConfig
import org.connecttag.lib.features.applock.AppLockBiometricResult
import org.connecttag.lib.features.applock.AppLockManager
import org.connecttag.lib.features.applock.AppLockUnlockResult
import org.connecttag.lib.features.applock.BiometricCapabilityChecker
import org.connecttag.lib.features.applock.PinConfigurationResult
import org.connecttag.lib.features.applock.PinNormalizer
import org.connecttag.lib.features.applock.PinScreenMode
import org.connecttag.lib.features.applock.PinSetupStep

private const val DEFAULT_PIN_LENGTH: Int = 4
private const val COUNTDOWN_TICK_MILLIS: Long = 250L
private const val MILLIS_PER_SECOND: Long = 1_000L
private const val HEADER_ICON_SIZE_DP: Int = 40
private const val HEADER_ITEM_SPACING_DP: Int = 12
private const val DOTS_TOP_SPACER_DP: Int = 16
private const val ERROR_BANNER_TOP_PADDING_DP: Int = 8
private const val ERROR_BANNER_HORIZONTAL_PADDING_DP: Int = 16
private const val ERROR_BANNER_VERTICAL_PADDING_DP: Int = 8
private const val SCREEN_HORIZONTAL_PADDING_DP: Int = 24
private const val SCREEN_VERTICAL_PADDING_DP: Int = 32

/**
 * Stateful Material 3 PIN lock screen bound directly to an [AppLockManager].
 *
 * Automatically determines whether a PIN is already configured (authentication mode),
 * needs to be created (two-step create + confirm flow), or changed ([PinScreenMode.CHANGE]),
 * supports anti-shoulder-surfing keypad scrambling ([scrambleKeypad]), integrates with
 * [AppLockBiometricAuthenticator], tracks failed attempts and cooldown timers, and invokes
 * [onAuthenticated], [onDuressAuthenticated], or [onPinCreated].
 */
@Composable
public fun PinLockScreen(
    manager: AppLockManager,
    modifier: Modifier = Modifier,
    mode: PinScreenMode? = null,
    pinLength: Int = DEFAULT_PIN_LENGTH,
    autoSubmit: Boolean = true,
    scrambleKeypad: Boolean? = null,
    showBiometricButton: Boolean = true,
    biometricPromptConfig: AppLockBiometricPromptConfig = AppLockBiometricPromptConfig(),
    onBiometricRequest: (() -> Unit)? = null,
    onAuthenticated: () -> Unit = {},
    onDuressAuthenticated: () -> Unit = {},
    onPinCreated: () -> Unit = {},
    onCancelSetup: (() -> Unit)? = null,
    colors: PinLockColors = PinLockDefaults.colors(),
    strings: PinLockStrings = PinLockStrings(),
    headerContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val snapshot by manager.snapshotFlow.collectAsState()
    val pinAlreadyExists = snapshot.policy.isMasterCredentialConfigured
    val effectiveMode = mode ?: if (pinAlreadyExists) {
        PinScreenMode.AUTHENTICATE
    } else {
        PinScreenMode.CREATE
    }
    var verifiedCurrentPin by rememberSaveable(effectiveMode) { mutableStateOf<String?>(null) }

    val biometricHardwareReady = remember(context) {
        BiometricCapabilityChecker(context).canAuthenticateBiometricOnly(
            strongOnly = biometricPromptConfig.strongAuthenticatorsOnly,
        )
    }
    val canShowBiometric = showBiometricButton &&
        snapshot.policy.allowBiometricUnlock &&
        (onBiometricRequest != null || (context is FragmentActivity && biometricHardwareReady))

    val effectiveBiometricClick: (() -> Unit)? = when {
        onBiometricRequest != null -> onBiometricRequest
        context is FragmentActivity && canShowBiometric -> {
            {
                AppLockBiometricAuthenticator.authenticate(
                    activity = context,
                    config = biometricPromptConfig,
                    manager = manager,
                ) { result ->
                    if (result is AppLockBiometricResult.Success) {
                        onAuthenticated()
                    }
                }
            }
        }
        else -> null
    }

    PinLockScreen(
        pinExists = pinAlreadyExists,
        mode = effectiveMode,
        modifier = modifier,
        pinLength = pinLength,
        autoSubmit = autoSubmit,
        scrambleKeypad = scrambleKeypad ?: snapshot.policy.scrambleKeypad,
        lockedOutUntilMillis = snapshot.attemptState.lockedOutUntilMillis,
        showBiometricButton = canShowBiometric,
        onBiometricClick = effectiveBiometricClick,
        onVerifyPin = { enteredPin ->
            val outcome = manager.evaluateCredential(enteredPin)
            when (outcome.result) {
                AppLockUnlockResult.MASTER -> {
                    verifiedCurrentPin = enteredPin
                    if (effectiveMode == PinScreenMode.AUTHENTICATE) {
                        onAuthenticated()
                    }
                    true
                }
                AppLockUnlockResult.DURESS -> {
                    if (effectiveMode == PinScreenMode.AUTHENTICATE) {
                        onDuressAuthenticated()
                        true
                    } else {
                        false
                    }
                }
                AppLockUnlockResult.LOCKED_OUT,
                AppLockUnlockResult.INVALID,
                -> false
            }
        },
        onCreatePin = { newPin ->
            val result = manager.configurePinCredentials(
                masterPin = newPin,
                currentPin = verifiedCurrentPin,
            )
            if (result is PinConfigurationResult.Success) {
                onPinCreated()
                onAuthenticated()
                true
            } else {
                false
            }
        },
        onCancelCreate = {
            if (effectiveMode == PinScreenMode.CREATE) {
                manager.disableAppLock(clearStoredCredentials = true)
            }
            onCancelSetup?.invoke()
        },
        colors = colors,
        strings = strings,
        headerContent = headerContent,
    )
}

/**
 * Flexible, stateless Material 3 PIN lock and creation screen that replaces external
 * `pin_lock_compose` libraries while supporting custom verification, two-step PIN creation,
 * three-step PIN change (`VERIFY_CURRENT` -> `ENTER_NEW` -> `CONFIRM_NEW`), cooldown lockouts,
 * anti-shoulder-surfing keypad scrambling ([scrambleKeypad]), and custom slot headers.
 */
@Composable
public fun PinLockScreen(
    pinExists: Boolean,
    onVerifyPin: (String) -> Boolean,
    onCreatePin: (String) -> Boolean,
    modifier: Modifier = Modifier,
    mode: PinScreenMode = if (pinExists) PinScreenMode.AUTHENTICATE else PinScreenMode.CREATE,
    pinLength: Int = DEFAULT_PIN_LENGTH,
    maxPinLength: Int = pinLength,
    autoSubmit: Boolean = true,
    scrambleKeypad: Boolean = false,
    lockedOutUntilMillis: Long? = null,
    showBiometricButton: Boolean = false,
    onBiometricClick: (() -> Unit)? = null,
    onPinIncorrect: () -> Unit = {},
    onCancelCreate: (() -> Unit)? = null,
    colors: PinLockColors = PinLockDefaults.colors(),
    strings: PinLockStrings = PinLockStrings(),
    headerContent: (@Composable () -> Unit)? = null,
    titleContent: (@Composable (PinScreenMode, PinSetupStep) -> Unit)? = null,
) {
    val initialStep = if (mode == PinScreenMode.CHANGE) {
        PinSetupStep.VERIFY_CURRENT
    } else {
        PinSetupStep.ENTER_NEW
    }
    var setupStep by rememberSaveable(pinExists, mode) { mutableStateOf(initialStep) }
    var firstDraftPin by rememberSaveable(pinExists, mode) { mutableStateOf("") }
    var enteredPin by rememberSaveable(pinExists, mode) { mutableStateOf("") }
    var errorMessage by rememberSaveable(pinExists, mode) { mutableStateOf<String?>(null) }
    var pinCreated by rememberSaveable(pinExists, mode) { mutableStateOf(false) }
    var scrambleEpoch by rememberSaveable(pinExists, mode) { mutableIntStateOf(0) }
    var remainingLockoutSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lockedOutUntilMillis) {
        while (true) {
            val deadline = lockedOutUntilMillis ?: 0L
            val remainingMillis = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
            remainingLockoutSeconds = if (remainingMillis > 0L) {
                (remainingMillis + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND
            } else {
                0L
            }
            if (remainingMillis <= 0L) break
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }

    val isLockedOut = remainingLockoutSeconds > 0L

    fun submitCurrentPin(candidate: String) {
        val normalized = PinNormalizer.normalize(candidate, digitsOnly = true)
        if (normalized.length < pinLength || isLockedOut) return

        when (mode) {
            PinScreenMode.AUTHENTICATE -> {
                if (onVerifyPin(normalized)) {
                    errorMessage = null
                    enteredPin = ""
                } else {
                    enteredPin = ""
                    errorMessage = strings.incorrectPinMessage
                    if (scrambleKeypad) {
                        scrambleEpoch += 1
                    }
                    onPinIncorrect()
                }
            }
            PinScreenMode.CREATE,
            PinScreenMode.CHANGE,
            -> {
                when (setupStep) {
                    PinSetupStep.VERIFY_CURRENT -> {
                        if (onVerifyPin(normalized)) {
                            errorMessage = null
                            enteredPin = ""
                            setupStep = PinSetupStep.ENTER_NEW
                        } else {
                            enteredPin = ""
                            errorMessage = strings.incorrectPinMessage
                            if (scrambleKeypad) {
                                scrambleEpoch += 1
                            }
                            onPinIncorrect()
                        }
                    }
                    PinSetupStep.ENTER_NEW -> {
                        firstDraftPin = normalized
                        enteredPin = ""
                        errorMessage = null
                        setupStep = PinSetupStep.CONFIRM_NEW
                    }
                    PinSetupStep.CONFIRM_NEW -> {
                        if (normalized == firstDraftPin) {
                            if (onCreatePin(normalized)) {
                                pinCreated = true
                                errorMessage = null
                                enteredPin = ""
                            } else {
                                enteredPin = ""
                                errorMessage = strings.invalidPinRuleMessage
                            }
                        } else {
                            firstDraftPin = ""
                            enteredPin = ""
                            setupStep = PinSetupStep.ENTER_NEW
                            errorMessage = strings.pinMismatchMessage
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = mode != PinScreenMode.AUTHENTICATE && !pinCreated && onCancelCreate != null) {
        if (setupStep == PinSetupStep.CONFIRM_NEW) {
            firstDraftPin = ""
            enteredPin = ""
            errorMessage = null
            setupStep = PinSetupStep.ENTER_NEW
        } else {
            onCancelCreate?.invoke()
        }
    }

    val activeErrorMessage = if (isLockedOut) {
        strings.formatLockedOutMessage(remainingLockoutSeconds)
    } else {
        errorMessage
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundColor)
            .safeDrawingPadding()
            .padding(
                horizontal = SCREEN_HORIZONTAL_PADDING_DP.dp,
                vertical = SCREEN_VERTICAL_PADDING_DP.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HEADER_ITEM_SPACING_DP.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (headerContent != null) {
                headerContent()
            } else {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.dotFilledColor,
                    modifier = Modifier.size(HEADER_ICON_SIZE_DP.dp),
                )
            }

            if (titleContent != null) {
                titleContent(mode, setupStep)
            } else {
                val resolvedTitle = when {
                    mode == PinScreenMode.AUTHENTICATE -> strings.enterPinTitle
                    setupStep == PinSetupStep.VERIFY_CURRENT -> strings.verifyCurrentPinTitle
                    setupStep == PinSetupStep.CONFIRM_NEW -> strings.confirmPinTitle
                    else -> strings.createPinTitle
                }
                val resolvedSubtitle = when {
                    mode == PinScreenMode.AUTHENTICATE -> strings.enterPinSubtitle
                    setupStep == PinSetupStep.VERIFY_CURRENT -> strings.enterPinSubtitle
                    setupStep == PinSetupStep.CONFIRM_NEW -> strings.confirmPinSubtitle
                    else -> strings.createPinSubtitle
                }

                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.titleColor,
                    textAlign = TextAlign.Center,
                )
                if (!resolvedSubtitle.isNullOrBlank()) {
                    Text(
                        text = resolvedSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.subtitleColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DOTS_TOP_SPACER_DP.dp))

            PinDotsIndicator(
                enteredLength = enteredPin.length,
                totalDots = pinLength,
                isError = activeErrorMessage != null,
                colors = colors,
            )

            AnimatedVisibility(
                visible = activeErrorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    color = colors.errorContainerColor,
                    contentColor = colors.onErrorContainerColor,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.padding(top = ERROR_BANNER_TOP_PADDING_DP.dp),
                ) {
                    Text(
                        text = activeErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            horizontal = ERROR_BANNER_HORIZONTAL_PADDING_DP.dp,
                            vertical = ERROR_BANNER_VERTICAL_PADDING_DP.dp,
                        ),
                    )
                }
            }
        }

        PinPad(
            enabled = !isLockedOut,
            scrambleDigits = scrambleKeypad && mode == PinScreenMode.AUTHENTICATE,
            scrambleSeed = scrambleEpoch,
            showBiometricButton = showBiometricButton && mode == PinScreenMode.AUTHENTICATE,
            onBiometricClick = onBiometricClick,
            showSubmitButton = !autoSubmit || maxPinLength > pinLength,
            canSubmit = enteredPin.length >= pinLength,
            onSubmitClick = { submitCurrentPin(enteredPin) },
            colors = colors,
            strings = strings,
            onDigitClick = { digit ->
                if (enteredPin.length < maxPinLength) {
                    errorMessage = null
                    val updated = enteredPin + digit.toString()
                    enteredPin = updated
                    if (autoSubmit && maxPinLength == pinLength && updated.length == pinLength) {
                        submitCurrentPin(updated)
                    }
                }
            },
            onBackspaceClick = {
                if (enteredPin.isNotEmpty()) {
                    errorMessage = null
                    enteredPin = enteredPin.dropLast(1)
                }
            },
        )
    }
}
