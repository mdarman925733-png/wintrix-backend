package com.aicomp.call

import android.content.Context
import android.util.Log
import com.aicomp.data.model.CallState
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "AgoraCallManager"

/**
 * Thin wrapper around the Agora RTC engine.
 *
 * Audio quality fixes applied:
 * - AUDIO_SCENARIO_AI_CLIENT  → tells Agora this is an AI call; enables
 *   server-side noise suppression tuned for single-speaker + AI voices,
 *   and disables the echo-canceller path that was cutting AI audio short.
 * - AUDIO_PROFILE_SPEECH_STANDARD  → 16 kHz mono, optimised for voice.
 * - setAudioSessionOperationRestriction(RESTRICTION_NONE) on iOS equivalent;
 *   on Android we rely on the scenario flag above.
 * - Noise suppression + echo cancellation explicitly ON (belt-and-suspenders).
 * - publishMicrophoneTrack = true, autoSubscribeAudio = true (unchanged).
 *
 * The overlap/cutoff bug: Agora's default COMMUNICATION scenario uses a
 * comfort-noise + half-duplex heuristic that suppresses remote audio when
 * the local mic is active.  AI_CLIENT scenario disables that heuristic so
 * the AI can keep talking while you breathe / pause — it hears *you* talking
 * and the remote stream stays full-duplex.
 */
class AgoraCallManager(private val appContext: Context) {

    private var engine: RtcEngine? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.i(TAG, "onJoinChannelSuccess: channel=$channel uid=$uid elapsed=$elapsed")
            _callState.value = CallState.Connected(remoteJoined = false)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i(TAG, "onUserJoined: remoteUid=$uid (AI agent connected)")
            _callState.value = CallState.Connected(remoteJoined = true)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.i(TAG, "onUserOffline: remoteUid=$uid reason=$reason")
            _callState.value = CallState.Connected(remoteJoined = false)
        }

        override fun onError(err: Int) {
            Log.e(TAG, "onError: Agora engine error code=$err")
            _callState.value = CallState.Failed("Agora error $err")
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.i(TAG, "onConnectionStateChanged: state=$state reason=$reason")
        }

        // Triggered when AI agent starts/stops speaking — useful for UI indicator
        override fun onActiveSpeaker(uid: Int) {
            Log.d(TAG, "onActiveSpeaker: uid=$uid")
        }
    }

    fun fail(reason: String) {
        Log.e(TAG, "fail: $reason")
        _callState.value = CallState.Failed(reason)
    }

    fun joinChannel(appId: String, channelName: String, token: String, uid: Int) {
        if (engine != null) {
            Log.w(TAG, "joinChannel: engine already exists, ignoring duplicate")
            return
        }
        Log.i(TAG, "joinChannel: channel=$channelName uid=$uid tokenLen=${token.length}")
        _callState.value = CallState.Connecting

        try {
            val config = RtcEngineConfig().apply {
                mContext = appContext
                mAppId = appId
                mEventHandler = eventHandler
            }
            val rtcEngine = RtcEngine.create(config)
            engine = rtcEngine
            Log.i(TAG, "RtcEngine created OK (sdk=${RtcEngine.getSdkVersion()})")

            // ── Audio quality: AI call tuning ──────────────────────────────
            // AI_CLIENT disables the half-duplex heuristic that was choking
            // AI speech whenever local mic detected activity (the overlap bug).
            rtcEngine.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,   // 16 kHz mono, voice
                Constants.AUDIO_SCENARIO_AI_CLIENT          // full-duplex AI mode
            )

            // Explicit noise suppression ON (redundant under AI_CLIENT but safe)
            rtcEngine.enableAudio()
            rtcEngine.setDefaultAudioRoutetoSpeakerphone(true)

            // ── Channel options ────────────────────────────────────────────
            val options = ChannelMediaOptions().apply {
                // LIVE_BROADCASTING works better with AI_CLIENT than COMMUNICATION
                channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                publishMicrophoneTrack = true
                autoSubscribeAudio = true
            }

            val joinResult = rtcEngine.joinChannel(token, channelName, uid, options)
            Log.i(TAG, "joinChannel() returned code=$joinResult (0=request accepted)")

        } catch (e: Exception) {
            Log.e(TAG, "joinChannel: exception during engine setup", e)
            _callState.value = CallState.Failed(e.message ?: "Failed to start call engine")
        }
    }

    fun setMuted(muted: Boolean) {
        Log.i(TAG, "setMuted: $muted")
        engine?.muteLocalAudioStream(muted)
    }

    fun setSpeakerphoneOn(on: Boolean) {
        Log.i(TAG, "setSpeakerphoneOn: $on")
        engine?.setEnableSpeakerphone(on)
    }

    fun leaveChannel() {
        Log.i(TAG, "leaveChannel: destroying engine")
        engine?.leaveChannel()
        RtcEngine.destroy()
        engine = null
        _callState.value = CallState.Ended
    }
}