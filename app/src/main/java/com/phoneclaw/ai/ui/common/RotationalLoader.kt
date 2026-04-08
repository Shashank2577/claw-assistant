package com.phoneclaw.ai.ui.common

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phoneclaw.ai.ui.theme.OpenClawAITheme

private const val DOT_COUNT = 3
private const val STAGGER_MS = 160

/**
 * Animated thinking/loading indicator.
 *
 * Displays [DOT_COUNT] pulsing dots in a row with a staggered scale animation,
 * plus an optional [text] label below them. The outer container also rotates
 * slowly using a [CubicBezierEasing] for a non-linear feel — mirroring the
 * gallery RotationalLoader motion language without requiring drawable assets.
 *
 * @param modifier  Layout modifier applied to the outer [Column].
 * @param text      Label shown below the dots. Pass empty string to hide it.
 * @param dotSize   Diameter of each dot.
 */
@Composable
fun RotationalLoader(
    modifier: Modifier = Modifier,
    text: String = "Thinking...",
    dotSize: Dp = 10.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotational_loader")

    val rotationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = CubicBezierEasing(0.5f, 0.16f, 0f, 0.71f)),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loader_rotation",
    )

    // Produce per-dot scale State objects with a staggered delay.
    val dotScales: List<State<Float>> = (0 until DOT_COUNT).map { index ->
        val delayMs = index * STAGGER_MS
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delayMs, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot_scale_$index",
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer { rotationZ = rotationProgress * 20f }
                .clearAndSetSemantics {},
        ) {
            dotScales.forEachIndexed { index, scaleState ->
                val scale = scaleState.value
                val color = when (index) {
                    0 -> MaterialTheme.colorScheme.primary
                    1 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.tertiary
                }
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "RotationalLoader - Default")
@Composable
private fun RotationalLoaderPreview() {
    OpenClawAITheme {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            RotationalLoader()
        }
    }
}

@Preview(showBackground = true, name = "RotationalLoader - No Text")
@Composable
private fun RotationalLoaderNoTextPreview() {
    OpenClawAITheme {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            RotationalLoader(text = "")
        }
    }
}
