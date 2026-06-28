package com.stylo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandPurple     = Color(0xFF7F77DD)
private val BrandDeepPurple = Color(0xFF26215C)
private val NeutralMid      = Color(0xFFD3D1C7)
private val NeutralDark     = Color(0xFF5F5E5A)

// ── Data ──────────────────────────────────────────────────────────────────────
private data class OOTDPost(
    val id: String,
    val userName: String,
    val avatarColor: Color,
    val caption: String,
    val imageColors: List<Color>,
    val timeAgo: String,
    val likeCount: Int,
    val isLiked: Boolean = false
)

private val samplePosts = listOf(
    OOTDPost(
        id = "1",
        userName = "Mia Chen",
        avatarColor = Color(0xFF7F77DD),
        caption = "Sunday brunch vibes ☀️ Kept it simple but polished today",
        imageColors = listOf(Color(0xFFE8D5B7), Color(0xFFD4B8C9), Color(0xFFB8C9D4)),
        timeAgo = "2h ago",
        likeCount = 24
    ),
    OOTDPost(
        id = "2",
        userName = "Sarah Kim",
        avatarColor = Color(0xFF4CAF50),
        caption = "All-black everything 🖤 Never gets old",
        imageColors = listOf(Color(0xFF5F5E5A)),
        timeAgo = "5h ago",
        likeCount = 41
    ),
    OOTDPost(
        id = "3",
        userName = "Lily Wang",
        avatarColor = Color(0xFFFF7043),
        caption = "Summer's not over for me 🌺 Grabbed this floral piece on sale",
        imageColors = listOf(
            Color(0xFFFFCDD2), Color(0xFFFFF9C4),
            Color(0xFFDCEDC8), Color(0xFFE1F5FE)
        ),
        timeAgo = "8h ago",
        likeCount = 18
    ),
    OOTDPost(
        id = "4",
        userName = "Emma Zhou",
        avatarColor = Color(0xFF9C27B0),
        caption = "Office chic 💼 Blazer szn is officially here",
        imageColors = listOf(Color(0xFFB8C9D4), Color(0xFFD4C4A8)),
        timeAgo = "1d ago",
        likeCount = 56
    ),
    OOTDPost(
        id = "5",
        userName = "Jade Liu",
        avatarColor = Color(0xFF26A69A),
        caption = "Thrifted this whole look for under \$30 🌿",
        imageColors = listOf(Color(0xFFB8D4C0), Color(0xFFD4B8C9), Color(0xFFE8D5B7)),
        timeAgo = "1d ago",
        likeCount = 33
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun OOTDFeedScreen(onBack: () -> Unit = {}) {
    val posts = remember { samplePosts.toMutableStateList() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = BrandDeepPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "OOTD Feed",
                    color = BrandDeepPurple,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(posts) { index, post ->
                    OOTDPostItem(
                        post = post,
                        onLikeToggle = {
                            val p = posts[index]
                            posts[index] = p.copy(
                                isLiked = !p.isLiked,
                                likeCount = if (p.isLiked) p.likeCount - 1 else p.likeCount + 1
                            )
                        }
                    )
                    if (index < posts.lastIndex) {
                        Divider(
                            modifier = Modifier.padding(start = 62.dp),
                            color = NeutralMid.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        // TODO: 接入 OOTD 发布流程
        FloatingActionButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = BrandPurple
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Post OOTD",
                tint = Color.White
            )
        }
    }
}

// ── Post item ─────────────────────────────────────────────────────────────────
@Composable
private fun OOTDPostItem(post: OOTDPost, onLikeToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(post.avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = post.userName.first().toString(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Content column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = post.userName,
                color = BrandPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = post.caption,
                color = NeutralDark,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            PostImageGrid(imageColors = post.imageColors)
            // TODO: 接入 AI 穿搭推荐
            Box(
                modifier = Modifier
                    .border(0.5.dp, BrandPurple.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {}
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "✨  Get this look",
                    color = BrandPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.timeAgo,
                    color = NeutralDark.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.clickable(onClick = onLikeToggle),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color(0xFFE57373) else NeutralDark.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = post.likeCount.toString(),
                        color = NeutralDark.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ── Image grid (single or multi) ──────────────────────────────────────────────
@Composable
private fun PostImageGrid(imageColors: List<Color>) {
    if (imageColors.isNotEmpty()) {
        when (imageColors.size) {
            // Single: full-width square — largest visual block
            1 -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(imageColors[0]),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(52.dp)
                )
            }
            // 3 images: full-width horizontal row, portrait thumbnails
            3 -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            // 2 or 4+ images: 2-column grid, square cells
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                imageColors.chunked(2).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        // Odd last row: right half stays empty to preserve 2-col alignment
                        if (rowColors.size == 1) Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
