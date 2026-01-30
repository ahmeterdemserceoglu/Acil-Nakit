package com.acilnakit.ui.designsystem

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.acilnakit.ui.theme.FluentBlue

/**
 * Fluent Design - Acrylic Material Simulation
 * Adaptive for Dark/Light mode.
 */
@Composable
fun Modifier.acrylicPanel(
    blurRadius: androidx.compose.ui.unit.Dp = 20.dp
) : Modifier {
    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    
    return this
        .clip(RoundedCornerShape(12.dp))
        .blur(blurRadius)
        .background(backgroundColor)
        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
}

/**
 * Premium Glass Card - Enhanced for Mica Effect using dynamic alphas
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.12f)
    val bottomColor = if (isDark) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.04f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(surfaceColor, bottomColor)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(borderColor, Color.Transparent)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Real-time Radar Pulse Animation
 */
@Composable
fun RadarPulse(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(100.dp)) {
            drawCircle(
                color = FluentBlue.copy(alpha = alpha),
                radius = size.minDimension / 2 * scale,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(FluentBlue, CircleShape)
                .border(2.dp, if (isSystemInDarkTheme()) Color.Black else Color.White, CircleShape)
        )
    }
}
/**
 * Fluent Design - Mica Material Simulation
 * Uses a slightly different alpha and layered approach for deep surfaces.
 */
@Composable
fun Modifier.micaSurface(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
) : Modifier {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = if (isDark) Color(0xFF202020) else Color(0xFFF3F3F3)
    val opacity = if (isDark) 0.8f else 0.9f
    
    return this
        .clip(shape)
        .background(surfaceColor.copy(alpha = opacity))
        .border(0.5.dp, Color.White.copy(alpha = if (isDark) 0.1f else 0.3f), shape)
}

/**
 * Trust & Rank Badges
 */
@Composable
fun Badge(
    text: String,
    icon: ImageVector,
    color: Color = FluentBlue
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Lottie Animation Wrapper for Micro-interactions
 */
@Composable
fun LottieInteraction(
    resId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = 1
) {
    val composition by com.airbnb.lottie.compose.rememberLottieComposition(
        com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(resId)
    )
    com.airbnb.lottie.compose.LottieAnimation(
        composition = composition,
        modifier = modifier,
        iterations = iterations
    )
}

/**
 * Mission Control - Task Progress Visualizer
 */
@Composable
fun MissionControlProgress(
    currentStatus: com.acilnakit.data.model.TaskStatus,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        com.acilnakit.data.model.TaskStatus.ASSIGNED to "Atandı",
        com.acilnakit.data.model.TaskStatus.IN_PROGRESS to "Başladı",
        com.acilnakit.data.model.TaskStatus.DELIVERED to "Teslim",
        com.acilnakit.data.model.TaskStatus.COMPLETED to "Tamam"
    )

    val currentStepIndex = steps.indexOfFirst { it.first == currentStatus }.let { 
        if (it == -1) {
            if (currentStatus == com.acilnakit.data.model.TaskStatus.COMPLETED) 3 else -1
        } else it
    }
    
    if (currentStepIndex == -1) return // Do not show if status is not in lifecycle

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, pair ->
            val isActive = index <= currentStepIndex
            val isCurrent = index == currentStepIndex
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (isActive) FluentBlue else Color.Gray.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    if (isActive && !isCurrent) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    } else if (isCurrent) {
                        Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pair.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) FluentBlue else Color.Gray,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            }
            
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.5f)
                        .background(if (index < currentStepIndex) FluentBlue else Color.Gray.copy(alpha = 0.2f))
                )
            }
        }
    }
}

