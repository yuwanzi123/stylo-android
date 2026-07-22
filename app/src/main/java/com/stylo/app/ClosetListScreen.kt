package com.stylo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand tokens (file-private, same values as HomeScreen) ───────────────────
private val BrandPurple      = Color(0xFF7F77DD)
private val BrandDeepPurple  = Color(0xFF26215C)
private val NeutralLight     = Color(0xFFF1EFE8)
private val NeutralMid       = Color(0xFFD3D1C7)
private val NeutralDark      = Color(0xFF5F5E5A)

private val categories = listOf(
    "All", "Tops", "Bottoms", "Outerwear", "Dresses", "Shoes", "Accessories"
)

// UI tab label -> backend ClosetItem.category value. Backend enum (top/bottom/
// outerwear/shoes/other) predates these tabs and is load-bearing for the outfit
// recommender, so we map here instead of changing it. Dresses/Accessories have
// no backend equivalent yet — intentionally unmapped, so selecting them shows empty.
private val categoryToBackendValue = mapOf(
    "Tops" to "top",
    "Bottoms" to "bottom",
    "Outerwear" to "outerwear",
    "Shoes" to "shoes",
)

// ── Root screen ───────────────────────────────────────────────────────────────
@Composable
fun ClosetListScreen(
    onBack: () -> Unit,
    onItemClick: (ClosetItem) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var searchText by remember { mutableStateOf("") }
    var closetItems by remember { mutableStateOf<List<ClosetItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val filteredItems = remember(closetItems, selectedCategory) {
        if (selectedCategory == "All") closetItems
        else closetItems.filter { it.category == categoryToBackendValue[selectedCategory] }
    }

    // 每次进入（含从详情页返回）都重新拉取列表数据
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refreshKey) {
        if (refreshKey == 0) return@LaunchedEffect
        isLoading = true
        StyloApi.fetchClosetItems { items, _ ->
            isLoading = false
            if (items != null) closetItems = items
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        ClosetTopBar(onBack = onBack)

        CategoryFilterRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        // Box takes all remaining space — Grid and SearchBar are Box siblings,
        // not sequential Column children, so SearchBar genuinely overlays the Grid.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandPurple, strokeWidth = 2.dp)
                }

                filteredItems.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Your closet is empty. Add some clothes to get started!",
                        color = NeutralDark,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredItems) { item ->
                        AsyncImage(
                            model = "${StyloApi.BASE_URL}${item.imageUrl}",
                            contentDescription = item.description,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onItemClick(item) },
                        )
                    }
                }
            }

            FloatingSearchBar(
                searchText = searchText,
                onSearchChange = { searchText = it },
                onSearch = {
                    // TODO: 接入语义搜索 API
                    android.util.Log.d("ClosetListScreen", "Search submitted: $searchText")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
private fun ClosetTopBar(onBack: () -> Unit) {
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
            text = "My Closet",
            color = BrandDeepPurple,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
    Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)
}

// ── Category filter row with right-edge fade ──────────────────────────────────
@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    // IntrinsicSize.Min lets fillMaxHeight() on the fade overlay resolve correctly
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                CategoryChip(
                    label = category,
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) }
                )
            }
        }

        // Fade gradient: only when more chips are scrollable to the right
        if (scrollState.canScrollForward) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White)
                        )
                    )
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            // border must come before clip so it isn't half-clipped
            .then(if (!selected) Modifier.border(1.dp, NeutralMid, shape) else Modifier)
            // background with shape param fills + clips without a separate clip call
            .background(if (selected) BrandPurple else Color.White, shape)
            // clip keeps the ripple effect contained to the shape
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else NeutralDark,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ── Floating search bar — semi-transparent pill, overlaid on the grid ────────
// Caller provides align(BottomCenter) + padding; this composable is just the pill shape.
@Composable
private fun FloatingSearchBar(
    searchText: String,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    val focusRequester = remember { FocusRequester() }
    // No shadow() here — shadow() forces a graphicsLayer that breaks semi-transparent
    // backgrounds on many devices. Border gives the floating feel without that side-effect.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeutralMid.copy(alpha = 0.6f), shape)
            .clip(shape)
            // alpha = 0.75f: grid cells must be visible through this during scroll
            .background(Color.White.copy(alpha = 0.75f))
            // Row had no pointer-consuming modifier of its own, so taps landing on the
            // padding/placeholder area (outside the BasicTextField's tight hit box) fell
            // through to the grid item beneath. Clickable both consumes those taps and
            // focuses the field, so the whole pill acts as the tap target, not just the
            // text glyphs.
            .clickable(onClick = { focusRequester.requestFocus() })
            .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (searchText.isEmpty()) {
                Text(
                    text = "Search e.g. pink hoodie, denim jacket…",
                    color = NeutralDark.copy(alpha = 0.45f),
                    fontSize = 14.sp
                )
            }
            // TODO: 接入语义搜索 API
            BasicTextField(
                value = searchText,
                onValueChange = onSearchChange,
                textStyle = TextStyle(color = BrandDeepPurple, fontSize = 14.sp),
                cursorBrush = SolidColor(BrandPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
        }

        // Search submit button — same visual language as HomeScreen send button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BrandPurple)
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
