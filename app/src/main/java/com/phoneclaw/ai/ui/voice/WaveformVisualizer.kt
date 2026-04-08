package com.phoneclaw.ai.ui.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val OrbPink = Color(0xFFEC4899)
private val OrbPurple = Color(0xFF7C3AED)
private val OrbPurpleLight = Color(0xFF9F67F5)

@Composable
fun WaveformVisualizer(
    state: VoiceState,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    when (state) {
        VoiceState.IDLE -> IdleOrbVisualizer(
            infiniteTransition = infiniteTransition,
            modifier = modifier,
        )
        VoiceState.LISTENING -> ListeningOrbVisualizer(
            infiniteTransition = infiniteTransition,
            modifier = modifier,
        )
        VoiceState.PROCESSING -> ProcessingOrbVisualizer(
            infiniteTransition = infiniteTransition,
            modifier = modifier,
        )
        VoiceState.SPEAKING -> SpeakingOrbVisualizer(
            infiniteTransition = infiniteTransition,
            modifier = modifier,
        )
    }
}

@Composable
private fun IdleOrbVisualizer(
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "idle_pulse",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(size.width, size.height) * 0.18f

        // Dark circular base
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = baseR * 1.6f,
            center = Offset(cx, cy),
        )

        // Outer glow rings
        for (i in 3 downTo 1) {
            drawCircle(
                color = OrbPurple.copy(alpha = 0.06f * i),
                radius = baseR * pulse * (1f + i * 0.35f),
                center = Offset(cx, cy),
            )
        }

        // Core orb — radial gradient simulated with layered circles
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbPink.copy(alpha = 0.5f), OrbPurple.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(cx, cy),
                radius = baseR * pulse,
            ),
            radius = baseR * pulse,
            center = Offset(cx, cy),
        )
    }
}

@Composable
private fun ListeningOrbVisualizer(
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "listening_pulse",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "listening_glow",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(size.width, size.height) * 0.22f

        // Dark circular base
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = baseR * 1.5f,
            center = Offset(cx, cy),
        )

        // Pulsing glow rings
        drawCircle(
            color = OrbPurple.copy(alpha = glowAlpha * 0.4f),
            radius = baseR * pulse * 1.5f,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = OrbPink.copy(alpha = glowAlpha * 0.3f),
            radius = baseR * pulse * 1.25f,
            center = Offset(cx, cy),
        )

        // Core orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    OrbPink,
                    OrbPurple,
                    OrbPurple.copy(alpha = 0f),
                ),
                center = Offset(cx, cy),
                radius = baseR * pulse,
            ),
            radius = baseR * pulse,
            center = Offset(cx, cy),
        )
    }
}

@Composable
private fun ProcessingOrbVisualizer(
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "processing_rotation",
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "processing_pulse",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(size.width, size.height) * 0.20f
        val orbitR = baseR * 1.4f
        val dotR = baseR * 0.12f
        val dotCount = 6

        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = baseR * 1.6f,
            center = Offset(cx, cy),
        )

        // Core orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbPurpleLight, OrbPurple, OrbPurple.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = baseR * pulse,
            ),
            radius = baseR * pulse,
            center = Offset(cx, cy),
        )

        // Orbiting dots
        for (i in 0 until dotCount) {
            val angle = rotation + (i * 2f * PI.toFloat() / dotCount)
            val x = cx + orbitR * cos(angle)
            val y = cy + orbitR * sin(angle)
            val alpha = 0.3f + 0.7f * (i.toFloat() / dotCount)
            drawCircle(
                color = OrbPink.copy(alpha = alpha),
                radius = dotR,
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun SpeakingOrbVisualizer(
    infiniteTransition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "speaking_pulse",
    )

    val outerPulse by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "speaking_outer",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(size.width, size.height) * 0.21f

        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = baseR * 1.7f,
            center = Offset(cx, cy),
        )

        // Expanding outer glow
        drawCircle(
            color = OrbPurple.copy(alpha = 0.2f),
            radius = baseR * outerPulse,
            center = Offset(cx, cy),
        )

        // Core orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    OrbPink,
                    OrbPurple,
                    OrbPurple.copy(alpha = 0f),
                ),
                center = Offset(cx, cy),
                radius = baseR * pulse,
            ),
            radius = baseR * pulse,
            center = Offset(cx, cy),
        )
    }
}
