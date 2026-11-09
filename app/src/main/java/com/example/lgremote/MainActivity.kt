package com.example.lgremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lgremote.client.WakeOnLan
import com.example.lgremote.client.WebOsClient
import com.example.lgremote.model.ConnectionState
import com.example.lgremote.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var client: WebOsClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = WebOsClient(this)

        setContent {
            LGRemoteTheme {
                MainScreen(client = client)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(client: WebOsClient) {
    val connectionState by client.connectionState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Touchpad, 1: Remote, 2: Apps
    var showConnectDialog by remember { mutableStateOf(false) }
    var tvIpAddress by remember { mutableStateOf("192.168.1.50") }
    var tvMacAddress by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LG 50UP7550PVG",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when (connectionState) {
                                            ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                            ConnectionState.CONNECTING, ConnectionState.WAITING_FOR_PAIRING -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (connectionState) {
                                    ConnectionState.CONNECTED -> "متصل (webOS 6.5.3)"
                                    ConnectionState.WAITING_FOR_PAIRING -> "وافق على الشاشة الآن (Allow)"
                                    ConnectionState.CONNECTING -> "جارٍ الاتصال..."
                                    ConnectionState.ERROR -> "فشل الاتصال - تأكد من الـ IP"
                                    ConnectionState.DISCONNECTED -> "غير متصل"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // WOL Power On Button
                    IconButton(onClick = {
                        if (tvMacAddress.isNotEmpty()) {
                            scope.launch {
                                val ok = WakeOnLan.wake(tvMacAddress)
                                snackbarHostState.showSnackbar(if (ok) "تم إرسال إشارة تشغيل الشاشة (WOL)" else "تأكد من عنوان الـ MAC")
                            }
                        } else {
                            showConnectDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Power, contentDescription = "Wake On LAN", tint = AccentCyan)
                    }

                    // Connection Settings
                    IconButton(onClick = { showConnectDialog = true }) {
                        Icon(
                            imageVector = if (connectionState == ConnectionState.CONNECTED) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Connection",
                            tint = if (connectionState == ConnectionState.CONNECTED) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Mouse, contentDescription = "Touchpad") },
                    label = { Text("الماوس (Magic)") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentCyan,
                        indicatorColor = LGRed
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.SettingsRemote, contentDescription = "Remote") },
                    label = { Text("الريموت") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentCyan,
                        indicatorColor = LGRed
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("البرامج") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = AccentCyan,
                        indicatorColor = LGRed
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            when (selectedTab) {
                0 -> TouchpadScreen(client = client)
                1 -> RemoteScreen(client = client)
                2 -> AppLauncherScreen(client = client)
            }

            if (connectionState == ConnectionState.WAITING_FOR_PAIRING) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF2E3B29),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF81C784), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ظهرت رسالة على شاشة التلفزيون الآن، اضغط على زر 'موافق' (Allow) في الريموت العادي لحفظ الاقتران.",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("إعدادات الاتصال بالشاشة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ادخل عنوان IP الخاص بالشاشة (تلاقيه في إعدادات الشاشة > الشبكة > اتصال Wi-Fi):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = tvIpAddress,
                        onValueChange = { tvIpAddress = it },
                        label = { Text("TV IP Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tvMacAddress,
                        onValueChange = { tvMacAddress = it },
                        label = { Text("MAC Address (لتشغيل الشاشة وهي مقفولة)") },
                        placeholder = { Text("مثال: AA:BB:CC:DD:EE:FF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConnectDialog = false
                        client.connect(tvIpAddress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LGRed)
                ) {
                    Text("اتصال الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
