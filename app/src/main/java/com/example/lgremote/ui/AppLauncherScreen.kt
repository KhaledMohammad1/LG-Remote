package com.example.lgremote.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lgremote.client.WebOsClient
import com.example.lgremote.model.TvApp

@Composable
fun AppLauncherScreen(client: WebOsClient) {
    val apps by client.installedApps.collectAsState()
    val view = LocalView.current

    // Quick predefined popular apps if list is loading
    val defaultPopularApps = listOf(
        TvApp("youtube.leanback.v4", "YouTube"),
        TvApp("netflix", "Netflix"),
        TvApp("shahid", "Shahid VIP"),
        TvApp("com.webos.app.browser", "Web Browser"),
        TvApp("com.webos.app.livetv", "Live TV"),
        TvApp("com.webos.app.hdmi1", "HDMI 1"),
        TvApp("com.webos.app.hdmi2", "HDMI 2"),
        TvApp("amazon", "Prime Video")
    )

    val displayList = if (apps.isNotEmpty()) apps else defaultPopularApps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "تطبيقات الشاشة (Apps)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اضغط على أي تطبيق لفتحه فوراً على التلفزيون",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    client.fetchInstalledApps()
                }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(displayList) { app ->
                AppCard(app = app) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.launchApp(app.id)
                }
            }
        }
    }
}

@Composable
fun AppCard(app: TvApp, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!app.iconUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = app.iconUrl,
                    contentDescription = app.title,
                    modifier = Modifier.size(44.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = app.title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
