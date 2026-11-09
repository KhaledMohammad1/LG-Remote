package com.example.lgremote.model

data class TvApp(
    val id: String,
    val title: String,
    val iconUrl: String? = null,
    val isSystemApp: Boolean = false
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    WAITING_FOR_PAIRING, // TV shows prompt "Allow"
    CONNECTED,
    ERROR
}

data class TvChannel(
    val channelId: String,
    val channelNumber: String,
    val channelName: String
)
