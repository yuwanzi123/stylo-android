package com.stylo.app

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandPurple     = Color(0xFF7F77DD)
private val BrandDeepPurple = Color(0xFF26215C)
private val NeutralMid      = Color(0xFFD3D1C7)
private val NeutralDark     = Color(0xFF5F5E5A)

@Composable
fun AddClothesScreen(
    hasSeenCaptureGuide: Boolean,
    onBack: () -> Unit,
    onNavigateToShootingGuide: () -> Unit,
    onLaunchCamera: () -> Unit,
    onUploadCompleted: () -> Unit = {},
) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("AddClothesScreen", "Gallery picked: $uri")
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = if (mimeType.startsWith("video/")) ".mp4" else ".jpg"
            CoroutineScope(Dispatchers.IO).launch {
                val tempFile = File(context.cacheDir, "gallery_upload$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                handleMediaUpload(context, tempFile, mimeType, onUploadCompleted)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AddClothesTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AddMethodCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Choose from gallery",
                subtitle = "Select a photo or video from your device",
                modifier = Modifier.weight(1f),
                onClick = {
                    galleryLauncher.launch(arrayOf("image/*", "video/*"))
                }
            )
            AddMethodCard(
                icon = Icons.Default.CameraAlt,
                title = "Capture new",
                subtitle = "Take a new photo or record a video",
                modifier = Modifier.weight(1f),
                onClick = {
                    if (hasSeenCaptureGuide) {
                        onLaunchCamera()
                    } else {
                        onNavigateToShootingGuide()
                    }
                }
            )
        }
    }
}

@Composable
private fun AddClothesTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = BrandDeepPurple
            )
        }
        Text(
            text = "Add Clothes",
            color = BrandDeepPurple,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, NeutralMid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandPurple.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandPurple,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = BrandDeepPurple,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = NeutralDark,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
