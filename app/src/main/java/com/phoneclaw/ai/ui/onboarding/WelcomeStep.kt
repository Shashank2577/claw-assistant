package com.phoneclaw.ai.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val Purple600 = Color(0xFF7C3AED)
private val Purple400 = Color(0xFF9F67F5)
private val LavenderBg = Color(0xFFF3EFFE)

@Composable
fun WelcomeStep(
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LavenderBg)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Purple gradient circle with chat icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Purple400, Purple600),
                        ),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Chat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "PocketAI",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your private AI assistant, right in your pocket",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DeviceIllustration()
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Purple400, Purple600),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DeviceIllustration(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp),
    ) {
        // Phone body
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
        )

        // Floating chat bubble - purple (top left)
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 24.dp)
                .offset(x = (-52).dp, y = (-48).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF7C3AED)),
        )

        // Floating pill - green (top right)
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 20.dp)
                .offset(x = 56.dp, y = (-36).dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF10B981)),
        )

        // Floating pill - pink (right middle)
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 20.dp)
                .offset(x = 62.dp, y = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEC4899)),
        )

        // Floating pill - blue (bottom left)
        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 20.dp)
                .offset(x = (-56).dp, y = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF3B82F6)),
        )

        // Screen content row 1 (inside phone)
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 12.dp)
                .offset(y = (-40).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEDE9FE)),
        )

        // Screen content row 2
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 12.dp)
                .offset(y = (-16).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF7C3AED).copy(alpha = 0.2f)),
        )

        // Screen content row 3
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 12.dp)
                .offset(y = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFEDE9FE)),
        )
    }
}
