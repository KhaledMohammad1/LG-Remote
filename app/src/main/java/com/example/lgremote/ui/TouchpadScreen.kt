package com.example.lgremote.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lgremote.client.WebOsClient

@Composable
fun TouchpadScreen(client: WebOsClient) {
    val view = LocalView.current
    var sensitivity by remember { mutableFloatStateOf(1.8f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Remote Header with Shortcuts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    client.sendPointerButton("BACK")
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(SurfaceDark, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Text(
                text = "Magic Touchpad (الماوس السحري)",
                style = MaterialTheme.typography.titleMedium,
                color = AccentCyan,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    client.sendPointerButton("HOME")
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(SurfaceDark, CircleShape)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Touchpad Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceDark)
                .border(2.dp, CardBackground, RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                            client.sendPointerClick()
                        }
                    )
                }
                .pointerInput(sensitivity) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            client.sendPointerMove(dragAmount.x * sensitivity, dragAmount.y * sensitivity)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Touch",
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "حرّك صباعك لتحريك مؤشر الماوس على الشاشة
المس نقرة واحدة للضغط (Click)",
                    color = TextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }

            // Scroll Wheel indicator on the right edge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .width(42.dp)
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground.copy(alpha = 0.7f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val scrollDelta = if (dragAmount.y > 0) -1 else 1
                            client.sendPointerScroll(scrollDelta)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Text("Scroll
عجلة", color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Touchpad Bottom Action Bar (Left Click, Enter, Scroll buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.sendPointerClick()
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LGRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("نقرة (OK)", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.sendPointerButton("MENU")
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = TextPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("القائمة", color = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sensitivity slider
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سرعة المؤشر:", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.8f..3.5f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan
                )
            )
        }
    }
}
