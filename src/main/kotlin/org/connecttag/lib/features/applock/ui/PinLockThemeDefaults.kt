package org.connecttag.lib.features.applock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Localizable text configuration for [PinLockScreen], [PinInputField], and [PinVerificationDialog].
 */
@Immutable
public data class PinLockStrings(
    public val enterPinTitle: String = DEFAULT_ENTER_PIN_TITLE,
    public val enterPinSubtitle: String? = null,
    public val createPinTitle: String = DEFAULT_CREATE_PIN_TITLE,
    public val createPinSubtitle: String? = DEFAULT_CREATE_PIN_SUBTITLE,
    public val confirmPinTitle: String = DEFAULT_CONFIRM_PIN_TITLE,
    public val confirmPinSubtitle: String? = DEFAULT_CONFIRM_PIN_SUBTITLE,
    public val verifyCurrentPinTitle: String = DEFAULT_VERIFY_CURRENT_PIN_TITLE,
    public val incorrectPinMessage: String = DEFAULT_INCORRECT_PIN_MESSAGE,
    public val pinMismatchMessage: String = DEFAULT_PIN_MISMATCH_MESSAGE,
    public val invalidPinRuleMessage: String = DEFAULT_INVALID_PIN_RULE_MESSAGE,
    public val lockedOutTemplate: String = DEFAULT_LOCKED_OUT_TEMPLATE,
    public val showPinContentDescription: String = DEFAULT_SHOW_PIN_DESCRIPTION,
    public val hidePinContentDescription: String = DEFAULT_HIDE_PIN_DESCRIPTION,
    public val backspaceContentDescription: String = DEFAULT_BACKSPACE_DESCRIPTION,
    public val biometricContentDescription: String = DEFAULT_BIOMETRIC_DESCRIPTION,
    public val confirmActionLabel: String = DEFAULT_CONFIRM_LABEL,
    public val cancelActionLabel: String = DEFAULT_CANCEL_LABEL,
    public val loadingSecurityLabel: String = DEFAULT_LOADING_SECURITY_LABEL,
    public val loadFailedSecurityLabel: String = DEFAULT_LOAD_FAILED_SECURITY_LABEL,
) {
    /**
     * Formats the lockout message with the remaining cooldown seconds.
     */
    public fun formatLockedOutMessage(remainingSeconds: Long): String =
        lockedOutTemplate.replace(SECONDS_PLACEHOLDER, remainingSeconds.coerceAtLeast(1L).toString())

    public companion object {
        public const val SECONDS_PLACEHOLDER: String = "{seconds}"

        public const val DEFAULT_ENTER_PIN_TITLE: String = "أدخل رمز PIN"
        public const val DEFAULT_CREATE_PIN_TITLE: String = "إنشاء رمز PIN"
        public const val DEFAULT_CREATE_PIN_SUBTITLE: String = "أدخل رمزاً رقمياً لحماية التطبيق"
        public const val DEFAULT_CONFIRM_PIN_TITLE: String = "تأكيد رمز PIN"
        public const val DEFAULT_CONFIRM_PIN_SUBTITLE: String = "أعد إدخال الرمز للتأكيد"
        public const val DEFAULT_VERIFY_CURRENT_PIN_TITLE: String = "أدخل رمز PIN الحالي"
        public const val DEFAULT_INCORRECT_PIN_MESSAGE: String = "رمز PIN غير صحيح"
        public const val DEFAULT_PIN_MISMATCH_MESSAGE: String = "رمزا PIN غير متطابقين، حاول مرة أخرى"
        public const val DEFAULT_INVALID_PIN_RULE_MESSAGE: String = "الرمز لا يستوفي شروط الأمان المطلوبة"
        public const val DEFAULT_LOCKED_OUT_TEMPLATE: String = "تم الحظر مؤقتاً، حاول بعد {seconds} ثانية"
        public const val DEFAULT_SHOW_PIN_DESCRIPTION: String = "إظهار الرمز"
        public const val DEFAULT_HIDE_PIN_DESCRIPTION: String = "إخفاء الرمز"
        public const val DEFAULT_BACKSPACE_DESCRIPTION: String = "حذف رقم"
        public const val DEFAULT_BIOMETRIC_DESCRIPTION: String = "البصمة"
        public const val DEFAULT_CONFIRM_LABEL: String = "تأكيد"
        public const val DEFAULT_CANCEL_LABEL: String = "إلغاء"
        public const val DEFAULT_LOADING_SECURITY_LABEL: String = "جاري التحقق من إعدادات الحماية..."
        public const val DEFAULT_LOAD_FAILED_SECURITY_LABEL: String = "تعذر تحميل إعدادات الأمان"

        /**
         * English preset for applications running in English locales.
         */
        public fun english(): PinLockStrings = PinLockStrings(
            enterPinTitle = "Enter PIN",
            createPinTitle = "Create PIN",
            createPinSubtitle = "Enter a numeric PIN to protect the app",
            confirmPinTitle = "Confirm PIN",
            confirmPinSubtitle = "Re-enter your PIN to confirm",
            verifyCurrentPinTitle = "Enter current PIN",
            incorrectPinMessage = "Incorrect PIN",
            pinMismatchMessage = "PINs do not match, please try again",
            invalidPinRuleMessage = "PIN does not meet security requirements",
            lockedOutTemplate = "Too many attempts. Try again in {seconds}s",
            showPinContentDescription = "Show PIN",
            hidePinContentDescription = "Hide PIN",
            backspaceContentDescription = "Backspace",
            biometricContentDescription = "Biometric unlock",
            confirmActionLabel = "Confirm",
            cancelActionLabel = "Cancel",
            loadingSecurityLabel = "Loading security policy...",
            loadFailedSecurityLabel = "Unable to load security settings",
        )
    }
}

/**
 * Customizable color palette for [PinLockScreen] and [PinPad].
 */
@Immutable
public data class PinLockColors(
    public val backgroundColor: Color,
    public val titleColor: Color,
    public val subtitleColor: Color,
    public val dotFilledColor: Color,
    public val dotEmptyColor: Color,
    public val errorColor: Color,
    public val errorContainerColor: Color,
    public val onErrorContainerColor: Color,
    public val keyContainerColor: Color,
    public val keyContentColor: Color,
    public val actionKeyContainerColor: Color,
    public val actionKeyContentColor: Color,
)

/**
 * Theme defaults for App Lock Compose components.
 */
public object PinLockDefaults {
    @Composable
    public fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surface,
        titleColor: Color = MaterialTheme.colorScheme.onSurface,
        subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        dotFilledColor: Color = MaterialTheme.colorScheme.primary,
        dotEmptyColor: Color = MaterialTheme.colorScheme.outlineVariant,
        errorColor: Color = MaterialTheme.colorScheme.error,
        errorContainerColor: Color = MaterialTheme.colorScheme.errorContainer,
        onErrorContainerColor: Color = MaterialTheme.colorScheme.onErrorContainer,
        keyContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        keyContentColor: Color = MaterialTheme.colorScheme.onSurface,
        actionKeyContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        actionKeyContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    ): PinLockColors = PinLockColors(
        backgroundColor = backgroundColor,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        dotFilledColor = dotFilledColor,
        dotEmptyColor = dotEmptyColor,
        errorColor = errorColor,
        errorContainerColor = errorContainerColor,
        onErrorContainerColor = onErrorContainerColor,
        keyContainerColor = keyContainerColor,
        keyContentColor = keyContentColor,
        actionKeyContainerColor = actionKeyContainerColor,
        actionKeyContentColor = actionKeyContentColor,
    )
}
