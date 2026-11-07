# App Lock UI Compose (`:features:app-lock-ui-compose`)

Jetpack Compose Material 3 UI components for PIN lock, PIN creation/confirmation/change, anti-shoulder-surfing scrambled keypads, live PIN rule validation, modal PIN verification and configuration dialogs, drop-in security settings cards, and fail-closed application security gates (`AppLockGate`).

## Key Components

- **`PinLockScreen`:** Drop-in Material 3 PIN lock, two-step PIN creation (`CREATE`), and three-step PIN change (`CHANGE`) screen replacing third-party `pin_lock_compose` dependencies, with built-in `AppLockBiometricAuthenticator` integration and anti-shoulder-surfing keypad scrambling.
- **`PinPad` & `PinDotsIndicator`:** Customizable numeric keypad with haptic feedback, `SecureRandom` digit scrambling (`scrambleDigits`), biometric action slot, and animated PIN progress indicators.
- **`PinInputField`, `PinRuleIndicator`, `PinValidationChecklist`, & `PinVerificationDialog`:** Reusable Outlined PIN text field with automatic Arabic-Indic digit normalization (`PinNormalizer`), live rule checklist indicators, and modal PIN verification dialog.
- **`PinConfigurationDialog` & `AppLockSettingsCard`:** Ready-to-use Material 3 settings card and configuration dialog for managing Master PIN, Duress PIN, biometric unlock, scrambled keypad, `FLAG_SECURE` recents protection, and panic lock gestures.
- **`AppLockGate`:** Fail-closed Compose wrapper that blocks navigation and screen rendering until authentication succeeds, with dedicated slots for custom disguise screens and Duress (`DECOY`) mode.
