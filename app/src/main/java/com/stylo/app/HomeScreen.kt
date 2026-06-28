package com.stylo.app

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Brand tokens ──────────────────────────────────────────────────────────────
private val BrandPurple     = Color(0xFF7F77DD)
private val BrandDeepPurple = Color(0xFF26215C)
private val BrandLightPurple = Color(0xFFEEEDFE)
private val NeutralLight    = Color(0xFFF1EFE8)
private val NeutralMid      = Color(0xFFD3D1C7)
private val NeutralDark     = Color(0xFF5F5E5A)
private val AmberAccent     = Color(0xFFFAC775)

// ── Sample conversation data ──────────────────────────────────────────────────
private sealed class ChatItem {
    data class UserMsg(val text: String) : ChatItem()
    data class AiMsg(val text: String) : ChatItem()
    object OutfitCard : ChatItem()
}

// TODO: 接入真实流行趋势图片数据（远期可能替换为商家推荐内容）
private val trendingColors = listOf(
    Color(0xFFE8D5B7),
    Color(0xFFB8C9D4),
    Color(0xFFD4B8C9),
    Color(0xFFB8D4C0),
)

private val homeAiReplies = listOf(
    "Great question! Here's what's catching my eye right now — effortlessly chic with a modern edge.",
    "Love that direction. Let me put something together based on what's been trending lately.",
    "On it! Here's a look that feels current and wearable at the same time.",
    "Perfect timing. Here's a suggestion that blends what's popular with what actually works day-to-day."
)

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    isLoggedIn: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToCloset: () -> Unit = {},
    onNavigateToAddClothes: () -> Unit = {},
    onNavigateToOOTDFeed: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember { mutableStateListOf<ChatItem>() }
    var isAiTyping by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var plusExpanded by remember { mutableStateOf(false) }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isAiTyping) return
        messages.add(ChatItem.UserMsg(trimmed))
        isAiTyping = true
        scope.launch {
            delay(900)
            val replyIdx = messages.count { it is ChatItem.AiMsg } % homeAiReplies.size
            messages.add(ChatItem.AiMsg(homeAiReplies[replyIdx]))
            isAiTyping = false
        }
    }

    LaunchedEffect(messages.size, isAiTyping) {
        if (messages.isNotEmpty()) {
            delay(50)
            val total = messages.size + (if (isAiTyping) 1 else 0)
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopBar(isLoggedIn = isLoggedIn, onAvatarClick = onNavigateToAccount)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (messages.isEmpty()) {
                item(key = "trending") { TrendingFashionCard() }
            }
            items(messages) { item ->
                when (item) {
                    is ChatItem.UserMsg -> UserBubble(item.text)
                    is ChatItem.AiMsg -> AiTextMessage(item.text)
                    ChatItem.OutfitCard -> OutfitCardPlaceholder()
                }
            }
            if (isAiTyping) {
                item(key = "typing") { HomeTypingIndicator() }
            }
        }

        // "+" menu — slides in above input row, full width minus 16dp margins
        AnimatedVisibility(
            visible = plusExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            PlusMenu(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                onDismiss = { plusExpanded = false },
                onNavigateToCloset = onNavigateToCloset,
                onNavigateToAddClothes = onNavigateToAddClothes,
                onNavigateToOOTDFeed = onNavigateToOOTDFeed
            )
        }

        // Anonymous quota banner (hidden once logged in)
        if (!isLoggedIn) {
            QuotaBanner(onSignIn = onNavigateToLogin)
        }

        InputRow(
            inputText = inputText,
            onInputChange = { inputText = it },
            plusExpanded = plusExpanded,
            onPlusToggle = { plusExpanded = !plusExpanded },
            onSend = {
                sendMessage(inputText)
                inputText = ""
            }
        )
    }
}

// ── Trending fashion card ─────────────────────────────────────────────────────
@Composable
private fun TrendingFashionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, NeutralMid)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Here's what's trending in women's fashion right now",
                color = NeutralDark,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(2) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(2) { colIndex ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(trendingColors[rowIndex * 2 + colIndex]),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "home-typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "home-typing-alpha"
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(text = "• • •", color = NeutralDark.copy(alpha = alpha), fontSize = 18.sp)
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(isLoggedIn: Boolean, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Stylo",
            color = BrandDeepPurple,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                // Logged-in: solid purple avatar background
                // Anonymous: semi-transparent purple with "?"
                .background(
                    if (isLoggedIn) BrandPurple else BrandPurple.copy(alpha = 0.2f)
                )
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Account",
                tint = if (isLoggedIn) Color.White else BrandPurple,
                modifier = Modifier.size(22.dp)
            )
        }
    }
    Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
}

// ── Chat bubbles ──────────────────────────────────────────────────────────────
@Composable
private fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = 16.dp, bottomEnd = 4.dp
                    )
                )
                .background(NeutralLight)   // #F1EFE8 — never purple
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = BrandDeepPurple, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun AiTextMessage(text: String) {
    // No bubble background — plain text, left-aligned
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = text,
            color = NeutralDark,            // #5F5E5A
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

// ── Outfit card (placeholder) ─────────────────────────────────────────────────
@Composable
private fun OutfitCardPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, NeutralMid)     // #D3D1C7
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Single outfit preview area — not individual item thumbnails
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandLightPurple),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "✦", color = BrandPurple, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Outfit preview",
                        color = BrandPurple.copy(alpha = 0.55f),
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Casual Weekend",
                color = BrandDeepPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "White linen shirt · Slim chinos · Clean sneakers",
                color = NeutralDark,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Anonymous quota banner ────────────────────────────────────────────────────
@Composable
private fun QuotaBanner(onSignIn: () -> Unit) {
    // TODO: 接入真实衣橱数据计数
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandLightPurple)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "3 / 5 closet items used",
            color = BrandDeepPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Sign in for unlimited →",
            color = BrandPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onSignIn)
        )
    }
}

// ── Bottom input row ──────────────────────────────────────────────────────────
@Composable
private fun InputRow(
    inputText: String,
    onInputChange: (String) -> Unit,
    plusExpanded: Boolean,
    onPlusToggle: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "+" / "×" button — same row as input, never floating above it
        PlusButton(isExpanded = plusExpanded, onClick = onPlusToggle)

        // Text input
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(NeutralLight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                Text(
                    text = "Ask about your style…",
                    color = NeutralDark.copy(alpha = 0.45f),
                    fontSize = 15.sp
                )
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = TextStyle(color = BrandDeepPurple, fontSize = 15.sp),
                cursorBrush = SolidColor(BrandPurple),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
        }

        // Send button — brand purple, circular
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BrandPurple)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Plus button with progress arc scaffold ────────────────────────────────────
@Composable
private fun PlusButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    // TODO: 接入真实处理进度 — pass 0f..1f when a job is running
    processingProgress: Float? = null
) {
    Box(
        modifier = Modifier.size(52.dp),   // outer canvas: 52dp, button: 44dp
        contentAlignment = Alignment.Center
    ) {
        // Progress arc drawn in the 4dp gap around the 44dp button
        if (processingProgress != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                drawArc(
                    color = BrandPurple,
                    startAngle = -90f,
                    sweepAngle = 360f * processingProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke)
                )
            }
        }

        // Inner button — brand purple (only purple spot in the input row besides send)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BrandPurple)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (isExpanded) "Close menu" else "Open menu",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Plus menu card ────────────────────────────────────────────────────────────
@Composable
private fun PlusMenu(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onNavigateToCloset: () -> Unit = {},
    onNavigateToAddClothes: () -> Unit = {},
    onNavigateToOOTDFeed: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            MenuRow(
                icon = Icons.Default.Add,
                iconTint = BrandPurple,
                title = "Add clothes",
                subtitle = "Upload to your closet",
                onClick = { onDismiss(); onNavigateToAddClothes() }
            )
            MenuRow(
                icon = Icons.Default.Checkroom,
                iconTint = BrandPurple,
                title = "My closet",
                subtitle = "Browse saved items",
                onClick = { onDismiss(); onNavigateToCloset() }
            )
            MenuRow(
                icon = Icons.Default.People,
                iconTint = BrandPurple,
                title = "OOTD Feed",
                subtitle = "See what others wear",
                onClick = { onDismiss(); onNavigateToOOTDFeed() }
            )
            // Amber icon is intentional — signals this photo is NOT saved to closet
            MenuRow(
                icon = Icons.Default.AddPhotoAlternate,
                iconTint = AmberAccent,
                title = "Add reference photo",
                subtitle = "Not saved to closet",
                onClick = {
                    // TODO: 触发参考图上传逻辑
                    Log.d("HomeScreen", "Add reference photo tapped")
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon in a soft-tinted circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        // Title + subtitle, left-aligned from same baseline
        Column {
            Text(
                text = title,
                color = BrandDeepPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = NeutralDark,
                fontSize = 13.sp
            )
        }
    }
}
