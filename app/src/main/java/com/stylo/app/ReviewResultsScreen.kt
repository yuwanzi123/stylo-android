package com.stylo.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val BrandPurple      = Color(0xFF7F77DD)
private val BrandDeepPurple  = Color(0xFF26215C)
private val NeutralMid       = Color(0xFFD3D1C7)
private val NeutralDark      = Color(0xFF5F5E5A)
private val AcceptGreen      = Color(0xFF4CAF50)
private val RejectRed        = Color(0xFFE53935)

@Composable
fun ReviewResultsScreen(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val context = LocalContext.current
    val items = PendingReviewResults.items
    val taskId = PendingReviewResults.taskId

    // true = accepted, false = rejected
    val accepted = remember { mutableStateListOf(*Array(items.size) { true }) }

    val acceptedCount = accepted.count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ReviewTopBar(onBack = onBack)

        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No items found", color = NeutralDark, fontSize = 15.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    ItemReviewCard(
                        item = item,
                        isAccepted = accepted[index],
                        onToggle = { accepted[index] = !accepted[index] },
                    )
                }
            }
        }

        Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
        ConfirmBar(
            acceptedCount = acceptedCount,
            onConfirm = {
                val toSubmit = items.filterIndexed { i, _ -> accepted[i] }
                confirmSelectedItems(toSubmit, taskId) { success ->
                    if (success) {
                        onConfirmed()
                    } else {
                        Toast.makeText(context, "Failed to save items, please retry", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun ReviewTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrandDeepPurple)
        }
        Text(
            text = "Review Results",
            color = BrandDeepPurple,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
}

@Composable
private fun ItemReviewCard(
    item: ReviewItem,
    isAccepted: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (isAccepted) 2.dp else 1.dp,
            color = if (isAccepted) BrandPurple else NeutralMid,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = StyloApi.BASE_URL + item.url,
                    contentDescription = item.description ?: item.filename,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                )

                // 右上角接受/取消切换按钮
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(
                            color = if (isAccepted) AcceptGreen else RejectRed,
                            shape = CircleShape,
                        ),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        imageVector = if (isAccepted) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (isAccepted) "Accepted" else "Rejected",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (!item.description.isNullOrEmpty()) {
                Text(
                    text = item.description,
                    color = NeutralDark,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ConfirmBar(
    acceptedCount: Int,
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
            enabled = acceptedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPurple,
                disabledContainerColor = NeutralMid,
            ),
        ) {
            Text(
                text = "Confirm ($acceptedCount item${if (acceptedCount != 1) "s" else ""})",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
