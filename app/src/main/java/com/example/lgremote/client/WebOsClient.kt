package com.example.lgremote.client

import android.content.Context
import android.util.Log
import com.example.lgremote.model.ConnectionState
import com.example.lgremote.model.TvApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebOsClient(private val context: Context) {

    private val TAG = "WebOsClient"
    private val PREFS_NAME = "LGRemotePrefs"
    private val KEY_CLIENT_KEY = "client_key"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val reqCounter = AtomicInteger(1)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _currentVolume = MutableStateFlow(15)
    val currentVolume: StateFlow<Int> = _currentVolume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _installedApps = MutableStateFlow<List<TvApp>>(emptyList())
    val installedApps: StateFlow<List<TvApp>> = _installedApps.asStateFlow()

    private val _currentAppId = MutableStateFlow("")
    val currentAppId: StateFlow<String> = _currentAppId.asStateFlow()

    private var mainWebSocket: WebSocket? = null
    private var pointerWebSocket: WebSocket? = null
    private var tvIp: String = ""

    private val okHttpClient: OkHttpClient by lazy {
        createUnsafeOkHttpClient()
    }

    private fun getSavedClientKey(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CLIENT_KEY, null)
    }

    private fun saveClientKey(key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CLIENT_KEY, key).apply()
    }

    /**
     * Connects to LG webOS TV.
     * webOS 6.5+ supports WSS on port 3001, falling back to WS on port 3000.
     */
    fun connect(ip: String) {
        tvIp = ip.trim()
        disconnect()
        _connectionState.value = ConnectionState.CONNECTING

        scope.launch {
            // First attempt WSS on port 3001 (standard on webOS 5.x & 6.x)
            val wssUrl = "wss://$tvIp:3001"
            val request = Request.Builder().url(wssUrl).build()

            mainWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Connected to $wssUrl")
                    sendRegistration(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMainSocketMessage(webSocket, text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WSS port 3001 failed: ${t.message}. Trying WS port 3000...")
                    // Fallback to WS port 3000
                    connectWsFallback()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            })
        }
    }

    private fun connectWsFallback() {
        val wsUrl = "ws://$tvIp:3000"
        val request = Request.Builder().url(wsUrl).build()

        mainWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected fallback to $wsUrl")
                sendRegistration(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMainSocketMessage(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed completely: ${t.message}")
                _connectionState.value = ConnectionState.ERROR
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        })
    }

    private fun sendRegistration(ws: WebSocket) {
        val clientKey = getSavedClientKey()
        val regJson = JSONObject().apply {
            put("id", "register_0")
            put("type", "register")
            put("payload", JSONObject().apply {
                put("forcePairing", false)
                put("pairingType", "PROMPT")
                if (!clientKey.isNullOrEmpty()) {
                    put("client-key", clientKey)
                }
                put("manifest", JSONObject().apply {
                    put("manifestVersion", 1)
                    put("appVersion", "1.0")
                    put("signed", JSONObject().apply {
                        put("created", "20240101")
                        put("appId", "com.example.lgremote")
                        put("vendorId", "com.example")
                        put("localizedAppNames", JSONObject().apply {
                            put("", "LG Magic Remote")
                            put("ar", "ريموت إل جي الذكي")
                        })
                        put("permissions", JSONArray(listOf(
                            "LAUNCH", "LAUNCH_WEBAPP", "APP_TO_APP", "CLOSE",
                            "CONTROL_AUDIO", "CONTROL_INPUT_TV", "CONTROL_POWER",
                            "READ_INSTALLED_APPS", "READ_CURRENT_CHANNEL", "READ_RUNNING_APPS",
                            "WRITE_NOTIFICATION_TOAST"
                        )))
                    })
                    put("permissions", JSONArray(listOf(
                        "LAUNCH", "CONTROL_AUDIO", "CONTROL_INPUT_TV", "READ_INSTALLED_APPS", "CONTROL_POWER"
                    )))
                })
            })
        }

        if (clientKey.isNullOrEmpty()) {
            _connectionState.value = ConnectionState.WAITING_FOR_PAIRING
        }
        ws.send(regJson.toString())
    }

    private fun handleMainSocketMessage(ws: WebSocket, text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            val id = json.optString("id")
            val payload = json.optJSONObject("payload")

            when (type) {
                "registered" -> {
                    val key = payload?.optString("client-key")
                    if (!key.isNullOrEmpty()) {
                        saveClientKey(key)
                    }
                    _connectionState.value = ConnectionState.CONNECTED
                    onTvAuthorized()
                }
                "response" -> {
                    if (id == "req_pointer" && payload != null) {
                        val socketPath = payload.optString("socketPath")
                        if (socketPath.isNotEmpty()) {
                            connectPointerSocket(socketPath)
                        }
                    } else if (id == "req_apps" && payload != null) {
                        parseApps(payload)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: $text", e)
        }
    }

    private fun onTvAuthorized() {
        // 1. Subscribe / request pointer socket for Magic Remote
        requestPointerSocket()
        // 2. Fetch installed apps list
        fetchInstalledApps()
        // 3. Show welcoming toast on TV
        showToast("تم الاتصال بـ ريموت Redmi A3 بنجاح")
    }

    // ==========================================
    // MAGIC REMOTE MOUSE POINTER SOCKET
    // ==========================================

    private fun requestPointerSocket() {
        sendRequest("req_pointer", "ssap://com.webos.service.networkinput/getPointerInputSocket")
    }

    private fun connectPointerSocket(socketPath: String) {
        Log.d(TAG, "Connecting to Pointer Socket: $socketPath")
        val req = Request.Builder().url(socketPath).build()
        pointerWebSocket = okHttpClient.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Magic Pointer Socket is Ready!")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Pointer socket error: ${t.message}")
            }
        })
    }

    /**
     * Move magic mouse pointer on TV screen.
     * @param dx relative X delta
     * @param dy relative Y delta
     */
    fun sendPointerMove(dx: Float, dy: Float) {
        val msg = "type:move
dx:${dx.toInt()}
dy:${dy.toInt()}
down:0

"
        pointerWebSocket?.send(msg)
    }

    /**
     * Mouse pointer click (selection)
     */
    fun sendPointerClick() {
        val msg = "type:click

"
        pointerWebSocket?.send(msg)
    }

    /**
     * Magic scroll wheel (up/down)
     */
    fun sendPointerScroll(dy: Int) {
        val msg = "type:scroll
dx:0
dy:$dy

"
        pointerWebSocket?.send(msg)
    }

    /**
     * Fast buttons sent via pointer socket
     */
    fun sendPointerButton(buttonName: String) {
        val msg = "type:button
name:$buttonName

"
        val sent = pointerWebSocket?.send(msg) ?: false
        if (!sent) {
            // Fallback via main socket SSAP if pointer socket not ready
            when (buttonName) {
                "HOME" -> sendRequest("btn_home", "ssap://system.launcher/open")
                "BACK" -> sendRequest("btn_back", "ssap://com.webos.service.networkinput/sendSpecialKey", JSONObject().put("key", "BACK"))
                "UP" -> sendRequest("btn_up", "ssap://com.webos.service.networkinput/sendSpecialKey", JSONObject().put("key", "UP"))
                "DOWN" -> sendRequest("btn_down", "ssap://com.webos.service.networkinput/sendSpecialKey", JSONObject().put("key", "DOWN"))
                "LEFT" -> sendRequest("btn_left", "ssap://com.webos.service.networkinput/sendSpecialKey", JSONObject().put("key", "LEFT"))
                "RIGHT" -> sendRequest("btn_right", "ssap://com.webos.service.networkinput/sendSpecialKey", JSONObject().put("key", "RIGHT"))
                "ENTER" -> sendRequest("btn_enter", "ssap://com.webos.service.networkinput/sendSpecialKey", JSONObject().put("key", "ENTER"))
            }
        }
    }

    // ==========================================
    // APPS CONTROL
    // ==========================================

    fun fetchInstalledApps() {
        sendRequest("req_apps", "ssap://com.webos.applicationManager/listLaunchPoints")
    }

    private fun parseApps(payload: JSONObject) {
        val array = payload.optJSONArray("launchPoints") ?: return
        val list = mutableListOf<TvApp>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val id = item.optString("id")
            val title = item.optString("title")
            val icon = item.optString("icon")
            val system = item.optBoolean("systemApp", false)
            list.add(TvApp(id = id, title = title, iconUrl = icon, isSystemApp = system))
        }
        _installedApps.value = list
    }

    fun launchApp(appId: String) {
        val payload = JSONObject().apply {
            put("id", appId)
        }
        sendRequest("launch_$appId", "ssap://system.launcher/launch", payload)
        _currentAppId.value = appId
    }

    fun closeApp(appId: String) {
        val payload = JSONObject().apply {
            put("id", appId)
        }
        sendRequest("close_$appId", "ssap://system.launcher/close", payload)
    }

    // ==========================================
    // AUDIO & CHANNELS & SYSTEM
    // ==========================================

    fun volumeUp() {
        sendRequest("vol_up", "ssap://audio/volumeUp")
    }

    fun volumeDown() {
        sendRequest("vol_down", "ssap://audio/volumeDown")
    }

    fun toggleMute(muted: Boolean) {
        _isMuted.value = muted
        val payload = JSONObject().put("mute", muted)
        sendRequest("mute", "ssap://audio/setMute", payload)
    }

    fun setVolume(level: Int) {
        _currentVolume.value = level
        val payload = JSONObject().put("volume", level)
        sendRequest("set_vol", "ssap://audio/setVolume", payload)
    }

    fun channelUp() {
        sendRequest("ch_up", "ssap://tv/channelUp")
    }

    fun channelDown() {
        sendRequest("ch_down", "ssap://tv/channelDown")
    }

    fun openChannel(channelNumber: String) {
        sendPointerButton(channelNumber)
    }

    fun turnOff() {
        sendRequest("power_off", "ssap://system/turnOff")
        disconnect()
    }

    fun play() {
        sendRequest("media_play", "ssap://media.controls/play")
    }

    fun pause() {
        sendRequest("media_pause", "ssap://media.controls/pause")
    }

    fun showToast(message: String) {
        val payload = JSONObject().put("message", message)
        sendRequest("toast", "ssap://system.notifications/createToast", payload)
    }

    fun sendSpecialKey(key: String) {
        sendPointerButton(key)
    }

    private fun sendRequest(id: String, uri: String, payload: JSONObject? = null) {
        val json = JSONObject().apply {
            put("id", id)
            put("type", "request")
            put("uri", uri)
            if (payload != null) {
                put("payload", payload)
            }
        }
        mainWebSocket?.send(json.toString())
    }

    fun disconnect() {
        try {
            pointerWebSocket?.close(1000, "Client disconnected")
            mainWebSocket?.close(1000, "Client disconnected")
        } catch (e: Exception) {
            // ignore
        }
        pointerWebSocket = null
        mainWebSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Trust-all SSL client for webOS 5/6 self-signed internal TLS certificate
     */
    private fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for websockets
            .build()
    }
}
