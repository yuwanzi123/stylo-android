package com.stylo.app

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import coil.compose.AsyncImage
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

// ── Brand tokens ──────────────────────────────────────────────────────────────
private val BrandPurple     = Color(0xFF7F77DD)
private val BrandDeepPurple = Color(0xFF26215C)
private val BrandLightPurple = Color(0xFFEEEDFE)
private val NeutralLight    = Color(0xFFF1EFE8)
private val NeutralMid      = Color(0xFFD3D1C7)
private val NeutralDark     = Color(0xFF5F5E5A)

// ── Conversation data ────────────────────────────────────────────────────────
private sealed class ChatItem {
    data class UserMsg(val text: String, val timestampMillis: Long = System.currentTimeMillis()) : ChatItem()
    data class AiMsg(val text: String, val timestampMillis: Long = System.currentTimeMillis()) : ChatItem()
    data class OutfitCardItem(
        val caption: String,
        val compositeImageUrl: String,
        val items: List<OutfitItem>,
        val timestampMillis: Long = System.currentTimeMillis(),
    ) : ChatItem()
}

private val ChatTimestampFormatter: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatChatTimestamp(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(ChatTimestampFormatter)

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    isLoggedIn: Boolean = false,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToCloset: () -> Unit = {},
    onNavigateToAddClothes: () -> Unit = {},
    onNavigateToOOTDFeed: () -> Unit = {},
    onNavigateToTheDrop: () -> Unit = {},
    onPickPhotoLibrary: () -> Unit = {},
    onPickCamera: () -> Unit = {},
    onPickFiles: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val userId = remember { StyloApi.getOrCreateUserId(context) }

    val messages = remember { mutableStateListOf<ChatItem>() }
    val historyMessages = remember { mutableStateListOf<ChatItem>() }
    var historyFetched by remember { mutableStateOf(false) }
    var historyVisible by remember { mutableStateOf(false) }
    var isAiTyping by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var plusExpanded by remember { mutableStateOf(false) }
    var isChatExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun collapseChat() {
        isChatExpanded = false
        focusManager.clearFocus()
    }

    // 历史消息只在首次展开时拉取一次并缓存到 historyMessages；之后的显示/隐藏切换只改 historyVisible，不再请求接口
    fun toggleHistory() {
        if (historyVisible) {
            historyVisible = false
            return
        }
        if (historyFetched) {
            historyVisible = true
            return
        }
        StyloApi.fetchChatHistory(userId) { history, _ ->
            historyMessages.clear()
            history?.forEach { m ->
                val ts = m.createdAtMillis ?: System.currentTimeMillis()
                when {
                    m.role == "user" -> historyMessages.add(ChatItem.UserMsg(m.content ?: "", ts))
                    m.type == "outfit_card" -> historyMessages.add(
                        ChatItem.OutfitCardItem(
                            caption = m.caption ?: "",
                            compositeImageUrl = m.compositeImageUrl ?: "",
                            items = m.items,
                            timestampMillis = ts,
                        )
                    )
                    else -> historyMessages.add(ChatItem.AiMsg(m.content ?: "", ts))
                }
            }
            historyFetched = true
            historyVisible = true
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || isAiTyping) return
        isChatExpanded = true
        messages.add(ChatItem.UserMsg(trimmed))
        isAiTyping = true
        StyloApi.sendChatMessage(userId, trimmed) { result, error ->
            isAiTyping = false
            if (result != null && result.type == "outfit_card") {
                messages.add(
                    ChatItem.OutfitCardItem(
                        caption = result.caption ?: "",
                        compositeImageUrl = result.compositeImageUrl ?: "",
                        items = result.items,
                        timestampMillis = result.createdAtMillis ?: System.currentTimeMillis(),
                    )
                )
            } else {
                messages.add(ChatItem.AiMsg(error ?: result?.caption ?: "Sorry, something went wrong. Please try again."))
            }
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
        TopBar(
            isLoggedIn = isLoggedIn,
            onAvatarClick = onNavigateToAccount
        )

        if (isChatExpanded) {
            FeatureGridCompact(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                onNavigateToAddClothes = onNavigateToAddClothes,
                onNavigateToCloset = onNavigateToCloset,
                onNavigateToOOTDFeed = onNavigateToOOTDFeed,
                onNavigateToTheDrop = onNavigateToTheDrop,
                onCollapse = ::collapseChat
            )
        } else {
            TopFeatureGrid(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                onNavigateToAddClothes = onNavigateToAddClothes,
                onNavigateToCloset = onNavigateToCloset,
                onNavigateToOOTDFeed = onNavigateToOOTDFeed,
                onNavigateToTheDrop = onNavigateToTheDrop
            )
        }

        AnimatedVisibility(
            visible = isChatExpanded,
            modifier = if (isChatExpanded) Modifier.weight(1f) else Modifier
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (historyVisible) {
                        items(historyMessages) { item -> ChatItemRow(item) }
                    }
                    items(messages) { item -> ChatItemRow(item) }
                    if (isAiTyping) {
                        item(key = "typing") { HomeTypingIndicator() }
                    }
                }

                // Pinned above the input row, doesn't scroll with the list
                Text(
                    text = if (historyVisible) "Hide history" else "View history",
                    color = BrandPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { toggleHistory() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
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
                onPickPhotoLibrary = onPickPhotoLibrary,
                onPickCamera = onPickCamera,
                onPickFiles = onPickFiles
            )
        }

        // Anonymous quota banner (hidden once logged in)
        if (!isLoggedIn) {
            QuotaBanner(onSignIn = onNavigateToLogin)
        }

        InputRow(
            inputText = inputText,
            onInputChange = { inputText = it },
            onFocusGained = { isChatExpanded = true },
            plusExpanded = plusExpanded,
            onPlusToggle = { plusExpanded = !plusExpanded },
            onSend = {
                sendMessage(inputText)
                inputText = ""
            }
        )
    }
}

// ── Top feature grid (main tabs) ──────────────────────────────────────────────
@Composable
private fun TopFeatureGrid(
    modifier: Modifier = Modifier,
    onNavigateToAddClothes: () -> Unit,
    onNavigateToCloset: () -> Unit,
    onNavigateToOOTDFeed: () -> Unit,
    onNavigateToTheDrop: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeatureTile(
                Icons.Default.Add, "Add Clothes",
                iconSize = 26.dp, fontSize = 13.sp,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = onNavigateToAddClothes
            )
            FeatureTile(
                Icons.Default.Checkroom, "My Closet",
                iconSize = 26.dp, fontSize = 13.sp,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = onNavigateToCloset
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeatureTile(
                Icons.Default.People, "OOTD Feed",
                iconSize = 26.dp, fontSize = 13.sp,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = onNavigateToOOTDFeed
            )
            FeatureTile(
                Icons.Default.Whatshot, "The Drop",
                iconSize = 26.dp, fontSize = 13.sp,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = onNavigateToTheDrop
            )
        }
    }
}

// 1x4 collapsed strip shown while the chat area is expanded — tiles stay tappable, just smaller.
@Composable
private fun FeatureGridCompact(
    modifier: Modifier = Modifier,
    onNavigateToAddClothes: () -> Unit,
    onNavigateToCloset: () -> Unit,
    onNavigateToOOTDFeed: () -> Unit,
    onNavigateToTheDrop: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureTile(
                Icons.Default.Add, "Add Clothes",
                iconSize = 18.dp,
                modifier = Modifier.weight(1f).height(56.dp),
                compact = true,
                onClick = onNavigateToAddClothes
            )
            FeatureTile(
                Icons.Default.Checkroom, "My Closet",
                iconSize = 18.dp,
                modifier = Modifier.weight(1f).height(56.dp),
                compact = true,
                onClick = onNavigateToCloset
            )
            FeatureTile(
                Icons.Default.People, "OOTD Feed",
                iconSize = 18.dp,
                modifier = Modifier.weight(1f).height(56.dp),
                compact = true,
                onClick = onNavigateToOOTDFeed
            )
            FeatureTile(
                Icons.Default.Whatshot, "The Drop",
                iconSize = 18.dp,
                modifier = Modifier.weight(1f).height(56.dp),
                compact = true,
                onClick = onNavigateToTheDrop
            )
        }

        // Collapse back to the 2x2 default grid — centered below the tile row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeutralLight)
                    .clickable(onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse to grid",
                    tint = BrandDeepPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureTile(
    icon: ImageVector,
    label: String,
    iconSize: Dp,
    fontSize: TextUnit = TextUnit.Unspecified,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(if (compact) 14.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandLightPurple),
        border = BorderStroke(1.dp, BrandPurple.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 4.dp else 12.dp, vertical = if (compact) 3.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrandPurple,
                modifier = Modifier.size(iconSize)
            )
            if (!compact) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    color = BrandDeepPurple,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
private fun TopBar(
    isLoggedIn: Boolean,
    onAvatarClick: () -> Unit
) {
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
    }
    Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
}

// ── Chat bubbles ──────────────────────────────────────────────────────────────
@Composable
private fun ChatItemRow(item: ChatItem) {
    when (item) {
        is ChatItem.UserMsg -> UserBubble(item.text, item.timestampMillis)
        is ChatItem.AiMsg -> AiTextMessage(item.text, item.timestampMillis)
        is ChatItem.OutfitCardItem -> OutfitCard(item)
    }
}

@Composable
private fun ChatTimestamp(millis: Long, alignEnd: Boolean) {
    Text(
        text = formatChatTimestamp(millis),
        color = NeutralMid,
        fontSize = 11.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 3.dp, start = if (alignEnd) 0.dp else 2.dp, end = if (alignEnd) 2.dp else 0.dp),
        textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    )
}

@Composable
private fun UserBubble(text: String, timestampMillis: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
        ChatTimestamp(timestampMillis, alignEnd = true)
    }
}

@Composable
private fun AiTextMessage(text: String, timestampMillis: Long) {
    // No bubble background — plain text, left-aligned
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text(
                text = text,
                color = NeutralDark,            // #5F5E5A
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.widthIn(max = 280.dp)
            )
        }
        ChatTimestamp(timestampMillis, alignEnd = false)
    }
}

// ── Outfit card ────────────────────────────────────────────────────────────────
@Composable
private fun OutfitCard(item: ChatItem.OutfitCardItem) {
    val imageUrl = "${StyloApi.BASE_URL}${item.compositeImageUrl}"
    var showFullscreenImage by remember { mutableStateOf(false) }

    if (showFullscreenImage) {
        Dialog(
            onDismissRequest = { showFullscreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ZoomableFullscreenImage(url = imageUrl, onDismiss = { showFullscreenImage = false })
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, NeutralMid)     // #D3D1C7
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (item.compositeImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Outfit preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandLightPurple)
                            .clickable { showFullscreenImage = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (item.caption.isNotEmpty()) {
                    Text(
                        text = item.caption,
                        color = NeutralDark,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
        ChatTimestamp(item.timestampMillis, alignEnd = false)
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
    onFocusGained: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (it.isFocused) onFocusGained() },
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
    onPickPhotoLibrary: () -> Unit = {},
    onPickCamera: () -> Unit = {},
    onPickFiles: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            MenuRow(
                icon = Icons.Default.PhotoLibrary,
                iconTint = BrandPurple,
                title = "Photo Library",
                subtitle = "Choose from your photos",
                onClick = { onDismiss(); onPickPhotoLibrary() }
            )
            MenuRow(
                icon = Icons.Default.CameraAlt,
                iconTint = BrandPurple,
                title = "Camera",
                subtitle = "Take a new photo or video",
                onClick = { onDismiss(); onPickCamera() }
            )
            MenuRow(
                icon = Icons.Default.Folder,
                iconTint = BrandPurple,
                title = "Files",
                subtitle = "Pick a file from your device",
                onClick = { onDismiss(); onPickFiles() }
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
