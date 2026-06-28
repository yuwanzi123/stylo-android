package com.stylo.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandPurple     = Color(0xFF7F77DD)
private val BrandDeepPurple = Color(0xFF26215C)
private val NeutralDark     = Color(0xFF5F5E5A)

private fun stepLabel(step: String?): String = when (step) {
    "processing_media"   -> "Processing your media..."
    "detecting_items"    -> "Detecting clothing items..."
    "generating_images"  -> "Generating flat-lay images..."
    else                 -> "Preparing..."
}

@Composable
fun ProcessingScreen() {
    val task = ProcessingTaskManager.currentTask

    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = BrandPurple,
            strokeWidth = 3.dp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Analyzing your wardrobe",
            color = BrandDeepPurple,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stepLabel(task?.currentStep),
            color = NeutralDark,
            fontSize = 14.sp,
        )
    }
}
