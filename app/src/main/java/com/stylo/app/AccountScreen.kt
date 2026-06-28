package com.stylo.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandPurple      = Color(0xFF7F77DD)
private val BrandDeepPurple  = Color(0xFF26215C)
private val NeutralMid       = Color(0xFFD3D1C7)
private val NeutralDark      = Color(0xFF5F5E5A)

@Composable
fun AccountScreen(
    isLoggedIn: Boolean = false,
    onBack: () -> Unit = {},
    onSignIn: () -> Unit = {}
) {
    val context = LocalContext.current

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
                text = "Account",
                color = BrandDeepPurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Divider(color = NeutralMid.copy(alpha = 0.5f), thickness = 0.5.dp)

        // User info section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BrandPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoggedIn) "U" else "?",
                    color = BrandPurple,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column {
                if (isLoggedIn) {
                    Text(
                        text = "Username",
                        color = BrandDeepPurple,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "user@email.com", color = NeutralDark, fontSize = 14.sp)
                } else {
                    Text(text = "Not signed in", color = NeutralDark, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in →",
                        color = BrandPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onSignIn)
                    )
                }
            }
        }

        Divider(color = NeutralMid.copy(alpha = 0.4f), thickness = 0.5.dp)

        // Menu items
        AccountMenuItem(
            icon = Icons.Default.Photo,
            label = "My OOTD Posts"
        ) { Toast.makeText(context, "My OOTD Posts — coming soon", Toast.LENGTH_SHORT).show() }

        AccountMenuItem(
            icon = Icons.Default.ChatBubble,
            label = "Chat History"
        ) { Toast.makeText(context, "Chat History — coming soon", Toast.LENGTH_SHORT).show() }

        AccountMenuItem(
            icon = Icons.Default.Star,
            label = "Upgrade to Premium"
        ) { Toast.makeText(context, "Upgrade to Premium — coming soon", Toast.LENGTH_SHORT).show() }

        AccountMenuItem(
            icon = Icons.Default.Settings,
            label = "Settings"
        ) { Toast.makeText(context, "Settings — coming soon", Toast.LENGTH_SHORT).show() }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = NeutralMid.copy(alpha = 0.4f), thickness = 0.5.dp)

        if (isLoggedIn) {
            AccountMenuItem(
                icon = Icons.Default.ExitToApp,
                label = "Sign out",
                labelColor = Color(0xFFB00020)
            ) { Toast.makeText(context, "Sign out — coming soon", Toast.LENGTH_SHORT).show() }
        } else {
            AccountMenuItem(
                icon = Icons.Default.Person,
                label = "Sign in",
                labelColor = BrandPurple,
                onClick = onSignIn
            )
        }
    }
}

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    label: String,
    labelColor: Color = BrandDeepPurple,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NeutralDark,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = labelColor,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = NeutralMid,
            modifier = Modifier.size(18.dp)
        )
    }
}
