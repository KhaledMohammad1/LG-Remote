package com.example.lgremote.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lgremote.client.WebOsClient

@Composable
fun RemoteScreen(client: WebOsClient) {
    val view = LocalView.current
    val isMuted by client.isMuted.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Power & Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    client.turnOff()
                },
                modifier = Modifier
                    .size(52.dp)
                    .background(LGRed.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, LGRed, CircleShape)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", tint = LGRed)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        client.sendPointerButton("INPUT")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Input, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المداخل (HDMI)", fontSize = 11.sp, color = TextPrimary)
                }

                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        client.sendPointerButton("SETTINGS")
                    },
                    modifier = Modifier.background(SurfaceDark, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Volume & Channel Rockers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Volume Rocker (Left)
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceDark)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        client.volumeUp()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Volume Up", tint = TextPrimary)
                }
                Text("VOL", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        client.volumeDown()
                    }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Volume Down", tint = TextPrimary)
                }
            }

            // Center: Mute & Home
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        client.toggleMute(!isMuted)
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(if (isMuted) LGRed else CardBackground, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        client.sendPointerButton("HOME")
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(CardBackground, CircleShape)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = AccentCyan)
                }
            }

            // Channel Rocker (Right)
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SurfaceDark)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        client.channelUp()
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Channel Up", tint = TextPrimary)
                }
                Text("CH", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                        client.channelDown()
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Channel Down", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // D-PAD Navigation Controller
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            // UP
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.sendPointerButton("UP")
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(56.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary, modifier = Modifier.size(32.dp))
            }

            // DOWN
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.sendPointerButton("DOWN")
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).size(56.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary, modifier = Modifier.size(32.dp))
            }

            // LEFT
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.sendPointerButton("LEFT")
                },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(56.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary, modifier = Modifier.size(32.dp))
            }

            // RIGHT
            IconButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                    client.sendPointerButton("RIGHT")
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(56.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary, modifier = Modifier.size(32.dp))
            }

            // CENTER OK BUTTON
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(LGRed)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        client.sendPointerButton("ENTER")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back, Exit & Play/Pause row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteMiniButton(Icons.Default.ArrowBack, "رجوع") {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                client.sendPointerButton("BACK")
            }
            RemoteMiniButton(Icons.Default.PlayArrow, "تشغيل") {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                client.play()
            }
            RemoteMiniButton(Icons.Default.Pause, "إيقاف") {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS)
                client.pause()
            }
            RemoteMiniButton(Icons.Default.Close, "خروج") {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                client.sendPointerButton("EXIT")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Color Buttons (Red, Green, Yellow, Blue)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ColorKeyButton(Color(0xFFE53935)) { client.sendPointerButton("RED") }
            ColorKeyButton(Color(0xFF43A047)) { client.sendPointerButton("GREEN") }
            ColorKeyButton(Color(0xFFFDD835)) { client.sendPointerButton("YELLOW") }
            ColorKeyButton(Color(0xFF1E88E5)) { client.sendPointerButton("BLUE") }
        }
    }
}

@Composable
fun RemoteMiniButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).background(SurfaceDark, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = TextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun ColorKeyButton(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(55.dp)
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable { onClick() }
    )
}
