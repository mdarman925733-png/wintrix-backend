package com.aicomp.ui.screens.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aicomp.call.AgoraCallManager
import com.aicomp.data.model.CallState
import com.aicomp.data.repository.ChatRepository
import com.aicomp.data.repository.CoinRepository
import com.aicomp.data.repository.CompanionRepository
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "CallViewModel"

/** Coins charged per minute of live call — first minute is charged upfront. */
private const val COINS_PER_MINUTE = 10L

class CallViewModel(application: Application) : AndroidViewModel(application) {

    private val callManager = AgoraCallManager(application.applicationContext)
    val callState: StateFlow<CallState> = callManager.callState

    private val functions = Firebase.functions
    private var agentId: String? = null
    private var currentUid: String? = null
    private var currentCompanionId: String? = null
    private var billingJob: Job? = null

    fun startCall(companionId: String) {
        viewModelScope.launch {
            val uid = ChatRepository.ensureSignedIn()
            currentUid = uid
            currentCompanionId = companionId

            // Charge the first minute upfront — refunded below if the call
            // never actually connects.
            if (!CoinRepository.trySpend(uid, COINS_PER_MINUTE)) {
                callManager.fail("Live call ke liye kam se kam $COINS_PER_MINUTE coins chahiye. Ad dekh kar coins kamao.")
                return@launch
            }

            try {
                val companion = CompanionRepository.getById(companionId)
                val voice = companion?.voice ?: "Leda"
                val baseInstructions = companion?.instructions ?: ""

                val userName = Firebase.auth.currentUser?.displayName?.takeIf { it.isNotBlank() }

                // ── Shared memory: inject recent chat+call history ──────────
                // Both Chat and Call write to the same DB path in ChatRepository,
                // so this history includes text chat AND previous call transcripts.
                val historyContext = ChatRepository.getRecentHistory(uid, companionId)

                val fullInstructions = buildString {
                    append(baseInstructions)
                    if (!userName.isNullOrBlank()) {
                        append("\n\nUser ka naam '${userName}' hai. Inhe naam se bulao.")
                    }
                    if (historyContext.isNotBlank()) {
                        append("\n\n")
                        append(historyContext)
                        append("\n\nYeh purani baatceet yaad rakho. Isko refer karo jab user kuch puchhe.")
                    }
                }

                Log.i(TAG, "startCall: uid=$uid companion=${companion?.name} voice=$voice historyLen=${historyContext.length}")

                val result = functions
                    .getHttpsCallable("startAgentCall")
                    .call(
                        mapOf(
                            "companionId" to companionId,
                            "voice" to voice,
                            "instructions" to fullInstructions
                        )
                    )
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.getData() as Map<String, Any?>
                val appId = data["appId"] as? String
                val channelName = data["channelName"] as? String
                val token = data["token"] as? String
                val callUid = (data["uid"] as? Number)?.toInt() ?: 1
                agentId = data["agentId"] as? String

                if (appId != null && channelName != null && !token.isNullOrEmpty()) {
                    callManager.joinChannel(appId, channelName, token, callUid)
                    startBilling(uid)
                } else {
                    CoinRepository.addCoins(uid, COINS_PER_MINUTE) // refund — call never started
                    callManager.fail("Server response was missing call details")
                }

            } catch (e: FirebaseFunctionsException) {
                Log.e(TAG, "startAgentCall FAILED — code=${e.code} message=${e.message}", e)
                CoinRepository.addCoins(uid, COINS_PER_MINUTE) // refund
                callManager.fail(e.message ?: "Couldn't start the call (${e.code})")
            } catch (e: Exception) {
                Log.e(TAG, "startCall failed", e)
                CoinRepository.addCoins(uid, COINS_PER_MINUTE) // refund
                callManager.fail(e.message ?: "Couldn't start the call")
            }
        }
    }

    /** Every 60s of connected call time, deduct another minute's coins. Ends the call if the user runs out. */
    private fun startBilling(uid: String) {
        billingJob?.cancel()
        billingJob = viewModelScope.launch {
            while (true) {
                delay(60_000)
                val spent = CoinRepository.trySpend(uid, COINS_PER_MINUTE)
                if (!spent) {
                    Log.i(TAG, "Coins khatam mid-call — ending call")
                    endCallSilently()
                    callManager.fail("Coins khatam ho gaye — call end ho gayi. Ad dekh kar aur coins kamao.")
                    break
                }
            }
        }
    }

    fun toggleMute(muted: Boolean) = callManager.setMuted(muted)
    fun toggleSpeaker(on: Boolean) = callManager.setSpeakerphoneOn(on)

    fun endCall() {
        billingJob?.cancel()
        billingJob = null
        endCallSilently()
    }

    /** Leaves the channel + stops the server agent, without touching billing/job state. */
    private fun endCallSilently() {
        callManager.leaveChannel()
        agentId?.let { id ->
            viewModelScope.launch {
                try {
                    functions.getHttpsCallable("stopAgentCall")
                        .call(mapOf("agentId" to id, "companionId" to currentCompanionId))
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "stopAgentCall failed (non-fatal)", e)
                }
            }
        }
        agentId = null
        currentCompanionId = null
    }

    override fun onCleared() {
        super.onCleared()
        endCall()
    }
}
