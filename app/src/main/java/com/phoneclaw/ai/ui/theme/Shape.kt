package com.phoneclaw.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Material3 shape scale (Claymorphism style). */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Claymorphism specific shapes
val ClayCardShape = RoundedCornerShape(32.dp)
val ClayButtonShape = RoundedCornerShape(20.dp)
val ClayInputShape = RoundedCornerShape(16.dp)
val ClayContainerShape = RoundedCornerShape(48.dp)
val ClayChipShape = RoundedCornerShape(20.dp)

/**
 * Chat bubble shape for the user side.
 * Updated for Claymorphism: 16dp corners except for the origin corner.
 */
val UserBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 4.dp,
)

/**
 * Chat bubble shape for the assistant side.
 * Updated for Claymorphism: 16dp corners except for the origin corner.
 */
val AssistantBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 16.dp,
)
