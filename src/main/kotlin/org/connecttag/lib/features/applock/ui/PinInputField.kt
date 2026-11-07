package org.connecttag.lib.features.applock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.connecttag.lib.features.applock.PinNormalizer
import org.connecttag.lib.features.applock.PinPolicyConfig
import org.connecttag.lib.features.applock.PinValidationStatus

/**
 * Outlined numeric PIN input field with automatic Unicode/Arabic-Indic digit normalization,
 * visibility toggle, and configurable length limit.
 *
 * Adapted and generalized from `StealthPinField` for reusable PIN setup and verification dialogs.
 */
@Composable
public fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    digitsOnly: Boolean = true,
    maxLength: Int = PinPolicyConfig.DEFAULT_MAX_PIN_LENGTH,
    imeAction: ImeAction = ImeAction.Done,
    showVisibilityToggle: Boolean = true,
    showPinContentDescription: String = PinLockStrings.DEFAULT_SHOW_PIN_DESCRIPTION,
    hidePinContentDescription: String = PinLockStrings.DEFAULT_HIDE_PIN_DESCRIPTION,
    onDone: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = { rawInput ->
            onValueChange(
                PinNormalizer.normalize(
                    rawValue = rawInput,
                    digitsOnly = digitsOnly,
                    maxLength = maxLength,
                ),
            )
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        label = { Text(text = label) },
        supportingText = supportingText?.let { message ->
            { Text(text = message) }
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (digitsOnly) KeyboardType.NumberPassword else KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() },
            onNext = { onNext?.invoke() },
        ),
        trailingIcon = if (showVisibilityToggle) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = if (visible) {
                            hidePinContentDescription
                        } else {
                            showPinContentDescription
                        },
                    )
                }
            }
        } else {
            null
        },
    )
}

/**
 * Single validation rule row with a checkmark icon indicating whether the rule is satisfied.
 */
@Composable
public fun PinRuleIndicator(
    label: String,
    satisfied: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RULE_ROW_SPACING_DP.dp),
    ) {
        Icon(
            imageVector = if (satisfied) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (satisfied) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(RULE_ICON_SIZE_DP.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (satisfied) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * Vertical checklist displaying live validation rules for a candidate PIN and optional Duress PIN.
 */
@Composable
public fun PinValidationChecklist(
    validationStatus: PinValidationStatus,
    lengthRuleLabel: String,
    nonTrivialRuleLabel: String? = null,
    modifier: Modifier = Modifier,
    confirmationMatches: Boolean? = null,
    confirmationRuleLabel: String? = null,
    duressDistinctRuleLabel: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CHECKLIST_SPACING_DP.dp),
    ) {
        PinRuleIndicator(
            label = lengthRuleLabel,
            satisfied = validationStatus.isLengthValid && validationStatus.isFormatValid,
        )
        if (!nonTrivialRuleLabel.isNullOrBlank()) {
            PinRuleIndicator(
                label = nonTrivialRuleLabel,
                satisfied = validationStatus.isNotTrivial,
            )
        }
        if (confirmationMatches != null && !confirmationRuleLabel.isNullOrBlank()) {
            PinRuleIndicator(
                label = confirmationRuleLabel,
                satisfied = confirmationMatches,
            )
        }
        if (!duressDistinctRuleLabel.isNullOrBlank()) {
            PinRuleIndicator(
                label = duressDistinctRuleLabel,
                satisfied = validationStatus.isDistinctFromPrimary,
            )
        }
    }
}

/**
 * Reusable dialog for verifying a PIN (e.g., before disabling app lock, exporting keys,
 * or unlocking a disguised stealth screen).
 */
@Composable
public fun PinVerificationDialog(
    title: String,
    description: String,
    pinValue: String,
    onPinValueChange: (String) -> Unit,
    pinLabel: String,
    confirmButtonText: String = PinLockStrings.DEFAULT_CONFIRM_LABEL,
    dismissButtonText: String = PinLockStrings.DEFAULT_CANCEL_LABEL,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    isConfirmEnabled: Boolean = pinValue.length >= PinPolicyConfig.DEFAULT_MIN_PIN_LENGTH,
    maxLength: Int = PinPolicyConfig.DEFAULT_MAX_PIN_LENGTH,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DIALOG_CONTENT_SPACING_DP.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PinInputField(
                    value = pinValue,
                    onValueChange = onPinValueChange,
                    label = pinLabel,
                    isError = !errorMessage.isNullOrBlank(),
                    maxLength = maxLength,
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (isConfirmEnabled) {
                            onConfirm()
                        }
                    },
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
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

private const val RULE_ROW_SPACING_DP: Int = 8
private const val RULE_ICON_SIZE_DP: Int = 16
private const val CHECKLIST_SPACING_DP: Int = 6
private const val DIALOG_CONTENT_SPACING_DP: Int = 10
