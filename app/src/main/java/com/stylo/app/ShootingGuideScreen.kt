package com.stylo.app

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private val BrandPurple      = Color(0xFF7F77DD)
private val BrandDeepPurple  = Color(0xFF26215C)
private val BrandLightPurple = Color(0xFFEEEDFE)
private val NeutralMid       = Color(0xFFD3D1C7)
private val NeutralDark      = Color(0xFF5F5E5A)

private data class GuidePage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private val guidePages = listOf(
    GuidePage(
        icon = Icons.Default.WbSunny,
        title = "Good lighting matters",
        body = "Poor light causes color shifts that confuse the recognition model. Natural daylight or bright indoor light works best."
    ),
    GuidePage(
        icon = Icons.Default.Speed,
        title = "Move slowly and steadily",
        body = "Fast motion blurs frames. The system drops blurry frames automatically, so slow sweeps mean better coverage."
    ),
    GuidePage(
        icon = Icons.Default.ViewModule,
        title = "Space items apart",
        body = "Overlapping clothes get merged into a single item. Lay them flat with a few centimetres between each piece."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShootingGuideScreen(
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { guidePages.size })
    val isLastPage = pagerState.currentPage == guidePages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = BrandDeepPurple
                )
            }
            TextButton(onClick = onFinish) {
                Text(
                    text = if (isLastPage) "Start" else "Skip",
                    color = BrandPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            GuidePageContent(page = guidePages[pageIndex])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(guidePages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (selected) BrandPurple else NeutralMid)
                )
            }
        }
    }
}

@Composable
private fun GuidePageContent(page: GuidePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BrandLightPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = BrandPurple,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            color = BrandDeepPurple,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.body,
            color = NeutralDark,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
