package org.connecttag.lib.features.applock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.connecttag.lib.features.applock.AppLockManager
import org.connecttag.lib.features.applock.PinConfigurationResult
import org.connecttag.lib.features.applock.PinPolicyConfig

/**
 * Modal dialog for configuring or updating the Master PIN and optional Duress/Decoy PIN
 * with live [PinValidationChecklist] feedback.
 */
@Composable
public fun PinConfigurationDialog(
    manager: AppLockManager,
    onDismiss: () -> Unit,
    onConfigured: () -> Unit = {},
    supportDuressPin: Boolean = true,
    title: String = DEFAULT_CONFIG_DIALOG_TITLE,
    currentPinLabel: String = DEFAULT_CURRENT_PIN_LABEL,
    masterPinLabel: String = DEFAULT_MASTER_PIN_LABEL,
    confirmPinLabel: String = DEFAULT_CONFIRM_PIN_LABEL,
    duressPinLabel: String = DEFAULT_DURESS_PIN_LABEL,
    lengthRuleLabel: String = DEFAULT_LENGTH_RULE_LABEL,
    confirmationRuleLabel: String = DEFAULT_CONFIRM_RULE_LABEL,
    duressDistinctRuleLabel: String = DEFAULT_DURESS_DISTINCT_RULE_LABEL,
    confirmButtonText: String = PinLockStrings.DEFAULT_CONFIRM_LABEL,
    dismissButtonText: String = PinLockStrings.DEFAULT_CANCEL_LABEL,
) {
    val snapshot by manager.snapshotFlow.collectAsState()
    val requiresCurrentPin = snapshot.policy.enabled && snapshot.policy.isMasterCredentialConfigured

    var currentPin by rememberSaveable { mutableStateOf("") }
    var masterPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var duressPin by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val masterStatus = manager.pinValidator.validate(
        rawPin = masterPin,
        primaryPinForComparison = duressPin.takeIf { it.isNotBlank() },
    )
    val confirmationMatches = masterPin.isNotEmpty() && masterPin == confirmPin
    val duressValidOrBlank = duressPin.isBlank() || manager.pinValidator.isValidPin(
        rawPin = duressPin,
        primaryPinForComparison = masterPin,
    )
    val canSubmit = (!requiresCurrentPin || currentPin.isNotBlank()) &&
        masterStatus.isValid &&
        confirmationMatches &&
        duressValidOrBlank

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SETTINGS_ITEM_SPACING_DP.dp)) {
                if (requiresCurrentPin) {
                    PinInputField(
                        value = currentPin,
                        onValueChange = {
                            currentPin = it
                            errorMessage = null
                        },
                        label = currentPinLabel,
                        maxLength = manager.pinValidator.config.maxPinLength,
                        imeAction = ImeAction.Next,
                    )
                }

                PinInputField(
                    value = masterPin,
                    onValueChange = {
                        masterPin = it
                        errorMessage = null
                    },
                    label = masterPinLabel,
                    maxLength = manager.pinValidator.config.maxPinLength,
                    imeAction = ImeAction.Next,
                )

                PinInputField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = it
                        errorMessage = null
                    },
                    label = confirmPinLabel,
                    maxLength = manager.pinValidator.config.maxPinLength,
                    imeAction = if (supportDuressPin) ImeAction.Next else ImeAction.Done,
                )

                if (supportDuressPin) {
                    PinInputField(
                        value = duressPin,
                        onValueChange = {
                            duressPin = it
                            errorMessage = null
                        },
                        label = duressPinLabel,
                        maxLength = manager.pinValidator.config.maxPinLength,
                        imeAction = ImeAction.Done,
                    )
                }

                PinValidationChecklist(
                    validationStatus = masterStatus,
                    lengthRuleLabel = lengthRuleLabel,
                    confirmationMatches = confirmationMatches,
                    confirmationRuleLabel = confirmationRuleLabel,
                    duressDistinctRuleLabel = if (supportDuressPin && duressPin.isNotBlank()) {
                        duressDistinctRuleLabel
                    } else {
                        null
                    },
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    val result = manager.configurePinCredentials(
                        masterPin = masterPin,
                        duressPin = duressPin.takeIf { it.isNotBlank() },
                        currentPin = currentPin.takeIf { requiresCurrentPin },
                        enableImmediately = true,
                    )
                    when (result) {
                        is PinConfigurationResult.Success -> {
                            onConfigured()
                            onDismiss()
                        }
                        PinConfigurationResult.CurrentPinInvalid -> {
                            errorMessage = PinLockStrings.DEFAULT_INCORRECT_PIN_MESSAGE
                        }
                        PinConfigurationResult.DuplicateMasterAndDuressPin -> {
                            errorMessage = duressDistinctRuleLabel
                        }
                        is PinConfigurationResult.MasterPinInvalid,
                        is PinConfigurationResult.DuressPinInvalid,
                        -> {
                            errorMessage = PinLockStrings.DEFAULT_INVALID_PIN_RULE_MESSAGE
                        }
                    }
                },
            ) {
                Text(text = confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissButtonText)
            }
        },
    )
}

/**
 * Drop-in Material 3 security settings card bound to [AppLockManager] for managing PIN lock,
 * Duress PIN, Biometric unlock, Anti-Shoulder-Surfing keypad scrambling, `FLAG_SECURE` recents protection,
 * and hardware panic lock gestures.
 */
@Composable
public fun AppLockSettingsCard(
    manager: AppLockManager,
    modifier: Modifier = Modifier,
    supportDuressPin: Boolean = true,
    showBiometricToggle: Boolean = true,
    showScrambleKeypadToggle: Boolean = true,
    showSecureRecentsToggle: Boolean = true,
    showPanicGestureToggle: Boolean = true,
    cardTitle: String = DEFAULT_CARD_TITLE,
    enableLockLabel: String = DEFAULT_ENABLE_LOCK_LABEL,
    changePinButtonLabel: String = DEFAULT_CHANGE_PIN_LABEL,
    biometricToggleLabel: String = DEFAULT_BIOMETRIC_TOGGLE_LABEL,
    scrambleKeypadToggleLabel: String = DEFAULT_SCRAMBLE_TOGGLE_LABEL,
    secureRecentsToggleLabel: String = DEFAULT_SECURE_RECENTS_TOGGLE_LABEL,
    panicGestureToggleLabel: String = DEFAULT_PANIC_TOGGLE_LABEL,
    disableDialogTitle: String = DEFAULT_DISABLE_DIALOG_TITLE,
    disableDialogDescription: String = DEFAULT_DISABLE_DIALOG_DESCRIPTION,
) {
    val snapshot by manager.snapshotFlow.collectAsState()
    val policy = snapshot.policy

    var showConfigDialog by rememberSaveable { mutableStateOf(false) }
    var showDisableVerifyDialog by rememberSaveable { mutableStateOf(false) }
    var disablePinInput by rememberSaveable { mutableStateOf("") }
    var disablePinError by rememberSaveable { mutableStateOf<String?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(SETTINGS_ITEM_SPACING_DP.dp),
        ) {
            Text(
                text = cardTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            SettingsSwitchRow(
                label = enableLockLabel,
                checked = policy.enabled,
                onCheckedChange = { shouldEnable ->
                    if (shouldEnable) {
                        showConfigDialog = true
                    } else if (policy.isMasterCredentialConfigured) {
                        disablePinInput = ""
                        disablePinError = null
                        showDisableVerifyDialog = true
                    } else {
                        manager.disableAppLock(clearStoredCredentials = false)
                    }
                },
            )

            if (policy.enabled) {
                OutlinedButton(
                    onClick = { showConfigDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = changePinButtonLabel)
                }

                HorizontalDivider()

                if (showBiometricToggle) {
                    SettingsSwitchRow(
                        label = biometricToggleLabel,
                        checked = policy.allowBiometricUnlock,
                        onCheckedChange = { enabled ->
                            manager.updatePolicySettings(allowBiometricUnlock = enabled)
                        },
                    )
                }

                if (showScrambleKeypadToggle) {
                    SettingsSwitchRow(
                        label = scrambleKeypadToggleLabel,
                        checked = policy.scrambleKeypad,
                        onCheckedChange = { enabled ->
                            manager.updatePolicySettings(scrambleKeypad = enabled)
                        },
                    )
                }

                if (showSecureRecentsToggle) {
                    SettingsSwitchRow(
                        label = secureRecentsToggleLabel,
                        checked = policy.secureRecentsEnabled,
                        onCheckedChange = { enabled ->
                            manager.updatePolicySettings(secureRecentsEnabled = enabled)
                        },
                    )
                }

                if (showPanicGestureToggle) {
                    SettingsSwitchRow(
                        label = panicGestureToggleLabel,
                        checked = policy.panicLockGestureEnabled,
                        onCheckedChange = { enabled ->
                            manager.updatePolicySettings(panicLockGestureEnabled = enabled)
                        },
                    )
                }
            }
        }
    }

    if (showConfigDialog) {
        PinConfigurationDialog(
            manager = manager,
            supportDuressPin = supportDuressPin,
            onDismiss = { showConfigDialog = false },
        )
    }

    if (showDisableVerifyDialog) {
        PinVerificationDialog(
            title = disableDialogTitle,
            description = disableDialogDescription,
            pinValue = disablePinInput,
            onPinValueChange = {
                disablePinInput = it
                disablePinError = null
            },
            pinLabel = DEFAULT_CURRENT_PIN_LABEL,
            errorMessage = disablePinError,
            maxLength = PinPolicyConfig.DEFAULT_MAX_PIN_LENGTH,
            onConfirm = {
                val disabled = manager.disableAppLock(
                    currentPin = disablePinInput,
                    requireCurrentPinIfEnabled = true,
                    clearStoredCredentials = false,
                )
                if (disabled) {
                    showDisableVerifyDialog = false
                    disablePinInput = ""
                    disablePinError = null
                } else {
                    disablePinError = PinLockStrings.DEFAULT_INCORRECT_PIN_MESSAGE
                }
            },
            onDismiss = {
                showDisableVerifyDialog = false
                disablePinInput = ""
                disablePinError = null
            },
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private const val CARD_PADDING_DP: Int = 16
private const val SETTINGS_ITEM_SPACING_DP: Int = 10

private const val DEFAULT_CONFIG_DIALOG_TITLE: String = "إعداد رمز الحماية (PIN)"
private const val DEFAULT_CURRENT_PIN_LABEL: String = "رمز PIN الحالي"
private const val DEFAULT_MASTER_PIN_LABEL: String = "رمز PIN الأساسي الجديد"
private const val DEFAULT_CONFIRM_PIN_LABEL: String = "تأكيد رمز PIN الأساسي"
private const val DEFAULT_DURESS_PIN_LABEL: String = "رمز الطوارئ/التمويه (اختياري)"
private const val DEFAULT_LENGTH_RULE_LABEL: String = "يتكون من 4 إلى 12 رقماً"
private const val DEFAULT_CONFIRM_RULE_LABEL: String = "تطابق الرمز الأساسي مع التأكيد"
private const val DEFAULT_DURESS_DISTINCT_RULE_LABEL: String = "رمز الطوارئ مختلف عن الرمز الأساسي"
private const val DEFAULT_CARD_TITLE: String = "حماية وقفل التطبيق"
private const val DEFAULT_ENABLE_LOCK_LABEL: String = "تفعيل قفل التطبيق برمز PIN"
private const val DEFAULT_CHANGE_PIN_LABEL: String = "تغيير رمز PIN أو رمز الطوارئ"
private const val DEFAULT_BIOMETRIC_TOGGLE_LABEL: String = "السماح بالفتح عبر البصمة"
private const val DEFAULT_SCRAMBLE_TOGGLE_LABEL: String = "بعثرة أزرار الأرقام لمنع التلصص"
private const val DEFAULT_SECURE_RECENTS_TOGGLE_LABEL: String = "منع لقطات الشاشة وإخفاء المعاينة"
private const val DEFAULT_PANIC_TOGGLE_LABEL: String = "تفعيل القفل الفوري السريع (Panic Lock)"
private const val DEFAULT_DISABLE_DIALOG_TITLE: String = "تأكيد إيقاف القفل"
private const val DEFAULT_DISABLE_DIALOG_DESCRIPTION: String = "أدخل رمز PIN الأساسي لإيقاف حماية التطبيق"
