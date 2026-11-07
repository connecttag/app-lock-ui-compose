package org.connecttag.lib.features.applock.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.security.SecureRandom

private const val DEFAULT_DOT_COUNT: Int = 4
private const val DOT_SIZE_FILLED_DP: Int = 16
private const val DOT_SIZE_EMPTY_DP: Int = 14
private const val DOT_SPACING_DP: Int = 16
private const val DOT_BORDER_WIDTH_DP: Int = 2
private const val KEY_SIZE_DP: Int = 72
private const val KEY_SPACING_DP: Int = 18
private const val MAX_PAD_WIDTH_DP: Int = 320
private const val FIRST_ROW_END_INDEX: Int = 3
private const val SECOND_ROW_END_INDEX: Int = 6
private const val THIRD_ROW_END_INDEX: Int = 9
private const val BOTTOM_DIGIT_INDEX: Int = 9

/**
 * Generates the 10-digit order (`[row1(3), row2(3), row3(3), bottomCenter(1)]`) for [PinPad].
 * When [scramble] is `true`, digits `0..9` are shuffled using [SecureRandom] to prevent
 * shoulder-surfing and finger-trace inference.
 */
public fun generatePinDigitOrder(
    scramble: Boolean,
    random: SecureRandom = SecureRandom(),
): List<Int> {
    val standardOrder = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
    if (!scramble) return standardOrder

    val mutableDigits = standardOrder.toMutableList()
    for (index in mutableDigits.lastIndex downTo 1) {
        val swapIndex = random.nextInt(index + 1)
        val temp = mutableDigits[index]
        mutableDigits[index] = mutableDigits[swapIndex]
        mutableDigits[swapIndex] = temp
    }
    return mutableDigits
}

/**
 * Animated indicator dots representing the number of entered PIN digits.
 */
@Composable
public fun PinDotsIndicator(
    enteredLength: Int,
    modifier: Modifier = Modifier,
    totalDots: Int = DEFAULT_DOT_COUNT,
    isError: Boolean = false,
    colors: PinLockColors = PinLockDefaults.colors(),
) {
    val effectiveTotal = totalDots.coerceAtLeast(enteredLength.coerceAtLeast(DEFAULT_DOT_COUNT))
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DOT_SPACING_DP.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(effectiveTotal) { index ->
            val isFilled = index < enteredLength
            val targetColor by animateColorAsState(
                targetValue = when {
                    isError -> colors.errorColor
                    isFilled -> colors.dotFilledColor
                    else -> colors.dotEmptyColor
                },
                label = "pinDotColor",
            )
            val dotSize by animateDpAsState(
                targetValue = if (isFilled) DOT_SIZE_FILLED_DP.dp else DOT_SIZE_EMPTY_DP.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "pinDotSize",
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .then(
                        if (isFilled || isError) {
                            Modifier.background(targetColor, CircleShape)
                        } else {
                            Modifier.border(DOT_BORDER_WIDTH_DP.dp, targetColor, CircleShape)
                        },
                    ),
            )
        }
    }
}

/**
 * Responsive Material 3 numeric keypad for PIN entry with haptic feedback, backspace,
 * anti-shoulder-surfing digit scrambling ([scrambleDigits]), optional biometric trigger,
 * and optional explicit submit action.
 */
@Composable
public fun PinPad(
    onDigitClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scrambleDigits: Boolean = false,
    scrambleSeed: Any? = null,
    showBiometricButton: Boolean = false,
    onBiometricClick: (() -> Unit)? = null,
    showSubmitButton: Boolean = false,
    canSubmit: Boolean = false,
    onSubmitClick: (() -> Unit)? = null,
    keySize: Dp = KEY_SIZE_DP.dp,
    colors: PinLockColors = PinLockDefaults.colors(),
    strings: PinLockStrings = PinLockStrings(),
) {
    val haptic = LocalHapticFeedback.current
    val orderedDigits = remember(scrambleDigits, scrambleSeed) {
        generatePinDigitOrder(scramble = scrambleDigits)
    }
    val rows = listOf(
        orderedDigits.subList(0, FIRST_ROW_END_INDEX),
        orderedDigits.subList(FIRST_ROW_END_INDEX, SECOND_ROW_END_INDEX),
        orderedDigits.subList(SECOND_ROW_END_INDEX, THIRD_ROW_END_INDEX),
    )
    val bottomCenterDigit = orderedDigits[BOTTOM_DIGIT_INDEX]

    Column(
        modifier = modifier.widthIn(max = MAX_PAD_WIDTH_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KEY_SPACING_DP.dp),
    ) {
        rows.forEach { rowDigits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowDigits.forEach { digit ->
                    PinDigitKey(
                        digit = digit,
                        enabled = enabled,
                        size = keySize,
                        colors = colors,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigitClick(digit)
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                showBiometricButton && onBiometricClick != null -> {
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBiometricClick()
                        },
                        enabled = enabled,
                        modifier = Modifier.size(keySize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.actionKeyContainerColor,
                            contentColor = colors.actionKeyContentColor,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = strings.biometricContentDescription,
                        )
                    }
                }
                showSubmitButton && onSubmitClick != null -> {
                    FilledTonalIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSubmitClick()
                        },
                        enabled = enabled && canSubmit,
                        modifier = Modifier.size(keySize),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colors.actionKeyContainerColor,
                            contentColor = colors.actionKeyContentColor,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = strings.confirmActionLabel,
                        )
                    }
                }
                else -> {
                    Spacer(modifier = Modifier.size(keySize))
                }
            }

            PinDigitKey(
                digit = bottomCenterDigit,
                enabled = enabled,
                size = keySize,
                colors = colors,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigitClick(bottomCenterDigit)
                },
            )

            FilledTonalIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspaceClick()
                },
                enabled = enabled,
                modifier = Modifier.size(keySize),
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = colors.keyContainerColor,
                    contentColor = colors.keyContentColor,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = strings.backspaceContentDescription,
                )
            }
        }
    }
}

@Composable
private fun PinDigitKey(
    digit: Int,
    enabled: Boolean,
    size: Dp,
    colors: PinLockColors,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = colors.keyContainerColor,
        contentColor = colors.keyContentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
