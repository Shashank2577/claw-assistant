package com.phoneclaw.ai.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phoneclaw.ai.data.db.entity.SpaceEntity
import com.phoneclaw.ai.ui.theme.*

@Composable
fun SpaceSwitcher(
    spaces: List<SpaceEntity>,
    selectedSpaceId: String?,
    onSpaceSelect: (String) -> Unit,
    onCreateSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(72.dp)
            .background(SurfaceCard)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(spaces, key = { it.id }) { space ->
                SpaceCircle(
                    name = space.name,
                    isSelected = space.id == selectedSpaceId,
                    onClick = { onSpaceSelect(space.id) }
                )
            }
        }

        IconButton(
            onClick = onCreateSpace,
            modifier = Modifier
                .size(48.dp)
                .background(BorderMuted.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Rounded.Add, null, tint = ForegroundSecondary)
        }
    }
}

@Composable
private fun SpaceCircle(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AccentViolet.copy(alpha = 0.1f), CircleShape)
                    .padding(2.dp)
                    .background(Color.Transparent, CircleShape)
            )
        }
        
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (isSelected) AccentViolet else SurfacePressed,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    color = if (isSelected) Color.White else ForegroundSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
