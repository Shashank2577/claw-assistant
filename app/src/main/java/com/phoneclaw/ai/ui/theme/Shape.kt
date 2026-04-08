package com.phoneclaw.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material3 shape scale. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Chat bubble shape for the user side.
 * Full rounding on top-left / top-right / bottom-left; tight bottom-right to
 * indicate message origin.
 */
val UserBubbleShape = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomStart = 12.dp,
    bottomEnd = 4.dp,
)

/**
 * Chat bubble shape for the assistant side.
 * Full rounding on top-left / top-right / bottom-right; tight bottom-left to
 * indicate message origin.
 */
val AssistantBubbleShape = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 12.dp,
    bottomStart = 4.dp,
    bottomEnd = 12.dp,
)
