package com.aicomp.data.model

sealed class CallState {
    object Idle : CallState()
    object Connecting : CallState()
    data class Connected(val remoteJoined: Boolean) : CallState()
    data class Failed(val reason: String) : CallState()
    object Ended : CallState()
}
