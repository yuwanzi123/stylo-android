package com.stylo.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// Fullscreen static image viewer: image is fixed in place, a single tap anywhere dismisses it.
// The full image is shown edge-to-edge (no cropping); leftover space is filled with a
// blurred, cropped copy of the same image instead of black bars.
// Usage: place inside Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))
@Composable
fun FullscreenImage(url: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Blurred backdrop (blur is a no-op below Android 12; the dim filter still applies)
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.25f), BlendMode.SrcAtop),
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp),
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
