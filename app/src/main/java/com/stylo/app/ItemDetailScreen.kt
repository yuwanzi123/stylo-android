package com.stylo.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Color tokens ───────────────────────────────────────────────────────────────
private val BrandPurple = Color(0xFF7F77DD)
private val BrandLightPurple = Color(0xFFEEEDFE)
private val BrandDeepPurple = Color(0xFF26215C)
private val NeutralLight = Color(0xFFF1EFE8)
private val NeutralMid = Color(0xFFD3D1C7)
private val NeutralDark = Color(0xFF5F5E5A)

// ── Edit mode data ─────────────────────────────────────────────────────────────
private data class EditChatMsg(val text: String, val isUser: Boolean)
private data class EditVersion(val id: Int, val color: Color)

private val EditVersionColors = listOf(
    Color(0xFFEEEDFE), // lavender
    Color(0xFFE8EDFE), // periwinkle
    Color(0xFFFEEDE8), // peach
    Color(0xFFEEFEED), // mint
    Color(0xFFFEF4E8), // warm cream
    Color(0xFFE8FEFD), // aqua
)

private val AiReplies = listOf(
    "Got it, adjusting the color now...",
    "Making that change — here's the updated version.",
    "Done! I've refined the tone based on your feedback.",
    "Updated. Let me know if you'd like further adjustments.",
)

// ── Root screen ────────────────────────────────────────────────────────────────
@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    onRetakePhoto: () -> Unit = {}
) {
    val context = LocalContext.current
    val item = SelectedClosetItem.current

    if (item == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val imageUrl = "${StyloApi.BASE_URL}${item.imageUrl}"
    var isEditMode by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var noteText by remember { mutableStateOf("") }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    if (showFullscreenImage) {
        Dialog(
            onDismissRequest = { showFullscreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            FullscreenImage(url = imageUrl, onDismiss = { showFullscreenImage = false })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = {
                Text(text = "Delete this item?", fontWeight = FontWeight.SemiBold, color = BrandDeepPurple)
            },
            text = {
                Text(text = "This action cannot be undone.", color = NeutralDark)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                            StyloApi.deleteClosetItem(item.id) { success ->
                                isDeleting = false
                                if (success) {
                                    showDeleteDialog = false
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Delete failed, please retry", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color(0xFFB00020),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Delete", color = Color(0xFFB00020))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = NeutralDark)
                }
            },
            containerColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (isEditMode) {
            EditMode(onBack = { isEditMode = false }, onRetakePhoto = onRetakePhoto)
        } else {
            ViewMode(
                imageUrl = imageUrl,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                noteText = noteText,
                onNoteChange = { noteText = it },
                onBack = onBack,
                onEditClick = { isEditMode = true },
                onImageClick = { showFullscreenImage = true },
                onDeleteClick = { showDeleteDialog = true }
            )
        }
    }
}

// ── Edit mode ──────────────────────────────────────────────────────────────────
@Composable
private fun EditMode(onBack: () -> Unit, onRetakePhoto: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember { mutableStateListOf<EditChatMsg>() }
    val versions = remember {
        mutableStateListOf(
            EditVersion(0, EditVersionColors[0]),
            EditVersion(1, EditVersionColors[1]),
            EditVersion(2, EditVersionColors[2]),
        )
    }
    var nextVersionId by remember { mutableIntStateOf(3) }
    var selectedVersionId by remember { mutableIntStateOf(0) }
    var hasStartedChat by remember { mutableStateOf(false) }
    var showSliders by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isAiTyping by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(0f) }
    var warmth by remember { mutableStateOf(0f) }

    // Auto-scroll chat to the latest item
    LaunchedEffect(messages.size, isAiTyping) {
        val total = messages.size + (if (isAiTyping) 1 else 0)
        if (total > 0) {
            delay(50) // let composition settle
            listState.animateScrollToItem(total - 1)
        }
    }

    val selectedVersion = versions.find { it.id == selectedVersionId } ?: versions.first()

    // Slider visual feedback overlaid on the image placeholder
    val brightnessOverlay = when {
        brightness > 0 -> Color.White.copy(alpha = brightness / 50f * 0.35f)
        brightness < 0 -> Color.Black.copy(alpha = -brightness / 50f * 0.35f)
        else -> Color.Transparent
    }
    val warmthOverlay = when {
        warmth > 0 -> Color(0xFFFAC775).copy(alpha = warmth / 50f * 0.28f)
        warmth < 0 -> Color(0xFF7799DD).copy(alpha = -warmth / 50f * 0.22f)
        else -> Color.Transparent
    }

    fun onSend() {
        val text = inputText.trim()
        if (text.isBlank() || isAiTyping) return
        messages.add(EditChatMsg(text, true))
        inputText = ""
        hasStartedChat = true
        showSliders = false // collapse sliders when chat starts (spec:互斥)
        isAiTyping = true
        scope.launch {
            delay(800)
            val replyIndex = messages.count { !it.isUser } % AiReplies.size
            messages.add(EditChatMsg(AiReplies[replyIndex], false))
            val nextColor = EditVersionColors[nextVersionId % EditVersionColors.size]
            versions.add(EditVersion(nextVersionId, nextColor))
            selectedVersionId = nextVersionId
            nextVersionId++
            isAiTyping = false
            // TODO: 接入图像生成 API，替换占位回复和占位图片
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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
                text = "Edit Item",
                color = BrandDeepPurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Image + version strip: large (pre-chat) vs compact (post-chat)
        if (!hasStartedChat) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(selectedVersion.color)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(brightnessOverlay))
                Box(modifier = Modifier.fillMaxSize().background(warmthOverlay))
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(versions, key = { it.id }) { version ->
                    VersionThumbnail(
                        version = version,
                        isSelected = version.id == selectedVersionId,
                        onSelect = { selectedVersionId = version.id },
                        onDelete = {
                            if (versions.size > 1) {
                                val idx = versions.indexOf(version)
                                versions.remove(version)
                                if (selectedVersionId == version.id) {
                                    selectedVersionId = versions[minOf(idx, versions.size - 1)].id
                                }
                            }
                            // TODO: 调用版本删除 API
                        }
                    )
                }
            }
        } else {
            // Post-chat header: 30% of screen height, fixed.
            // Image takes weight(1f) inside so it fills whatever remains after the version strip,
            // with no fixed-size floor that would leave empty space around it.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(selectedVersion.color)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(brightnessOverlay))
                    Box(modifier = Modifier.fillMaxSize().background(warmthOverlay))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(versions, key = { it.id }) { version ->
                        val isSelected = version.id == selectedVersionId
                        CompactVersionThumbnail(
                            version = version,
                            isSelected = isSelected,
                            onSelect = { selectedVersionId = version.id },
                            onDelete = {
                                if (versions.size > 1) {
                                    val idx = versions.indexOf(version)
                                    versions.remove(version)
                                    if (selectedVersionId == version.id) {
                                        selectedVersionId = versions[minOf(idx, versions.size - 1)].id
                                    }
                                }
                                // TODO: 调用版本删除 API
                            },
                            brightnessOverlay = if (isSelected) brightnessOverlay else Color.Transparent,
                            warmthOverlay = if (isSelected) warmthOverlay else Color.Transparent
                        )
                    }
                }
            }
            Divider(color = NeutralMid.copy(alpha = 0.4f), thickness = 0.5.dp)
        }

        // Chat LazyColumn is ALWAYS a direct weight(1f) child of the outer Column —
        // never inside a conditional branch — so Compose can measure it correctly
        // every time. Pre-chat: empty (acts as Spacer). Post-chat: shows messages.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasStartedChat) {
                items(messages) { msg ->
                    if (msg.isUser) EditUserBubble(msg.text)
                    else EditAiMessage(msg.text)
                }
                if (isAiTyping) {
                    item(key = "typing") { TypingIndicator() }
                }
            }
        }

        Divider(color = NeutralMid.copy(alpha = 0.4f), thickness = 0.5.dp)

        // Chat input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        text = "Tell AI what's wrong with the color",
                        color = NeutralDark.copy(alpha = 0.45f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(color = BrandDeepPurple, fontSize = 15.sp),
                    cursorBrush = SolidColor(BrandPurple),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrandPurple)
                    .clickable { onSend() },
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

        // "Adjust manually" link + expandable sliders
        // The link always stays visible; sliders are forced closed when chat starts
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Adjust manually",
                color = BrandPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { showSliders = !showSliders }
                    .padding(vertical = 8.dp)
            )
            AnimatedVisibility(
                visible = showSliders,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    SliderRow(label = "Brightness", value = brightness, onValueChange = { brightness = it })
                    SliderRow(label = "Saturation", value = saturation, onValueChange = { saturation = it })
                    SliderRow(label = "Warmth", value = warmth, onValueChange = { warmth = it })
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Retake photo link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Retake photo",
                color = NeutralDark,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onRetakePhoto)
                // TODO: 处理 retake 后的新照片（更新当前图片或加入版本历史）
            )
        }
    }
}

// ── Version thumbnail ──────────────────────────────────────────────────────────
@Composable
private fun VersionThumbnail(
    version: EditVersion,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    // Outer box is slightly larger to accommodate the × button overflowing the top-right
    Box(modifier = Modifier.size(72.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isSelected)
                        Modifier.border(2.dp, BrandPurple, RoundedCornerShape(8.dp))
                    else
                        Modifier
                )
                .background(version.color)
                .clickable(onClick = onSelect)
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(NeutralDark)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete version",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ── Compact version thumbnail (used in chat state — smaller, ~54dp) ────────────
@Composable
private fun CompactVersionThumbnail(
    version: EditVersion,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    brightnessOverlay: Color = Color.Transparent,
    warmthOverlay: Color = Color.Transparent
) {
    Box(modifier = Modifier.size(62.dp)) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (isSelected)
                        Modifier.border(2.dp, BrandPurple, RoundedCornerShape(6.dp))
                    else
                        Modifier
                )
                .background(version.color)
                .clickable(onClick = onSelect)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(brightnessOverlay))
            Box(modifier = Modifier.fillMaxSize().background(warmthOverlay))
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(NeutralDark)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete version",
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// ── Slider row ─────────────────────────────────────────────────────────────────
@Composable
private fun SliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = NeutralDark,
            fontSize = 13.sp,
            modifier = Modifier.width(82.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -50f..50f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = BrandPurple,
                activeTrackColor = BrandPurple,
                inactiveTrackColor = NeutralMid
            )
        )
        Text(
            text = "${value.toInt()}",
            color = NeutralDark,
            fontSize = 12.sp,
            modifier = Modifier
                .width(36.dp)
                .padding(start = 8.dp)
        )
    }
}

// ── Typing indicator ───────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing-alpha"
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = "• • •",
            color = NeutralDark.copy(alpha = alpha),
            fontSize = 18.sp
        )
    }
}

// ── Edit mode chat bubbles (match HomeScreen style exactly) ────────────────────
@Composable
private fun EditUserBubble(text: String) {
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
                .background(NeutralLight)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = text, color = BrandDeepPurple, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun EditAiMessage(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = text,
            color = NeutralDark,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.widthIn(max = 280.dp)
        )
    }
}

// ── View mode ──────────────────────────────────────────────────────────────────
@Composable
private fun ViewMode(
    imageUrl: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    noteText: String,
    onNoteChange: (String) -> Unit,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onImageClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
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
            Spacer(modifier = Modifier.weight(1f))
        }

        // Image section with floating edit icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onImageClick)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandLightPurple),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f))
                    .clickable(onClick = onEditClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit item",
                    tint = BrandDeepPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Worn badge
        // TODO: 接入 OOTD 穿着次数统计
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BrandLightPurple)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Worn 3 times · last on Jun 20",
                color = BrandPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tag switcher chips
        // TODO: 接入衣物分类标签数据
        val tabs = listOf("Category", "Color", "Season")
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) BrandPurple else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selected) BrandPurple else NeutralMid,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else NeutralDark,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Shared content area (Category / Color / Season)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .defaultMinSize(minHeight = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NeutralLight)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            when (selectedTab) {
                0 -> Text(
                    text = "Tops",
                    color = BrandDeepPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                1 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Pink", "White").forEach { colorName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BrandLightPurple)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = colorName,
                                color = BrandPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                2 -> Text(
                    text = "Spring · Summer",
                    color = BrandDeepPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notes — directly editable inline, no full edit mode needed
        // TODO: 保存备注到后端
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Notes",
                color = BrandDeepPurple,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            BasicTextField(
                value = noteText,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeutralLight)
                    .padding(12.dp),
                decorationBox = { inner ->
                    Box {
                        if (noteText.isEmpty()) {
                            Text(
                                text = "Add a note about this item...",
                                color = NeutralMid,
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Delete link — low emphasis, neutral gray, no button background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Delete item",
                color = NeutralDark,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onDeleteClick)
            )
        }
    }
}
