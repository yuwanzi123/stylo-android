package com.stylo.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val BrandPurple     = Color(0xFF7F77DD)
private val BrandDeepPurple = Color(0xFF26215C)
private val NeutralMid      = Color(0xFFD3D1C7)
private val NeutralDark     = Color(0xFF5F5E5A)
private val NeutralLight    = Color(0xFFF1EFE8)
private val RejectRed       = Color(0xFFE57373)

@Composable
fun ReviewResultsScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val context = LocalContext.current
    val items = PendingReviewResults.items
    val taskId = PendingReviewResults.taskId
    val totalCount = items.size

    val accepted = remember { mutableStateListOf(*Array(items.size) { true }) }
    val acceptedCount = accepted.count { it }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ReviewTopBar(onBack = onBack, totalCount = totalCount)

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No items found", color = NeutralDark, fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    ItemReviewRow(
                        item = item,
                        index = index,
                        isAccepted = accepted[index],
                        onAccept = { accepted[index] = true },
                        onReject = { accepted[index] = false },
                        onImageClick = { fullscreenImageUrl = StyloApi.BASE_URL + item.url },
                    )
                }
            }
        }

        // 说明文字
        Text(
            text = "✓ Accept · ✕ Remove — edit details later in Item Detail",
            color = NeutralDark,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)

        ConfirmBar(
            acceptedCount = acceptedCount,
            isSubmitting = isSubmitting,
            onConfirm = {
                isSubmitting = true
                val toSubmit = items.filterIndexed { i, _ -> accepted[i] }
                confirmSelectedItems(toSubmit, taskId) { success ->
                    if (success) {
                        onConfirmed()
                    } else {
                        isSubmitting = false
                        Toast.makeText(context, "Failed to save items, please retry", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    // 全屏大图查看 Dialog
    fullscreenImageUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { fullscreenImageUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewTopBar(onBack: () -> Unit, totalCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandDeepPurple)
        }
        Column {
            Text(
                text = "Review New Items",
                color = BrandDeepPurple,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "We found $totalCount item${if (totalCount != 1) "s" else ""} — confirm or remove",
                color = NeutralDark,
                fontSize = 12.sp,
            )
        }
    }
    Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
private fun ItemReviewRow(
    item: ReviewItem,
    index: Int,
    isAccepted: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onImageClick: () -> Unit,
) {
    val label = item.description?.takeIf { it.isNotEmpty() } ?: "Item ${index + 1}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isAccepted) BrandPurple.copy(alpha = 0.4f) else NeutralMid),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧缩略图（点击查看大图，与右侧 ✓/✕ 是独立点击区域）
            AsyncImage(
                model = StyloApi.BASE_URL + item.url,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(if (isAccepted) 1f else 0.45f)
                    .clickable(onClick = onImageClick),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 中间描述文字
            Text(
                text = label,
                color = if (isAccepted) NeutralDark else NeutralMid,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isAccepted) null else TextDecoration.LineThrough,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧 ✓ / ✕ 按钮竖排
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StateButton(
                    icon = Icons.Default.Check,
                    contentDescription = "Accept",
                    filled = isAccepted,
                    fillColor = BrandPurple,
                    onClick = onAccept,
                )
                StateButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Reject",
                    filled = !isAccepted,
                    fillColor = RejectRed,
                    onClick = onReject,
                )
            }
        }
    }
}

@Composable
private fun StateButton(
    icon: ImageVector,
    contentDescription: String,
    filled: Boolean,
    fillColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .then(
                if (filled)
                    Modifier.background(fillColor, CircleShape)
                else
                    Modifier.border(1.5.dp, NeutralMid, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) Color.White else NeutralMid,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun ConfirmBar(
    acceptedCount: Int,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Button(
            onClick = onConfirm,
            enabled = acceptedCount > 0 && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPurple,
                disabledContainerColor = NeutralMid,
            ),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = if (acceptedCount > 0)
                        "Add $acceptedCount item${if (acceptedCount != 1) "s" else ""} to closet"
                    else
                        "No items selected",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}
