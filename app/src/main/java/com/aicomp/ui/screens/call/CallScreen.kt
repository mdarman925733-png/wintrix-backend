package com.aicomp.ui.screens.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aicomp.data.model.CallState
import com.aicomp.data.repository.CompanionRepository
import com.aicomp.ui.components.CoinBadge
import com.aicomp.ui.components.CompanionAvatar
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    companionId: String,
    onEndCall: () -> Unit,
    viewModel: CallViewModel = viewModel()
) {
    val companion = CompanionRepository.getById(companionId)
    val context = LocalContext.current
    val callState by viewModel.callState.collectAsState()

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var micGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) viewModel.startCall(companionId)
    }

    LaunchedEffect(Unit) {
        if (micGranted) viewModel.startCall(companionId)
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.endCall() }
    }

    LaunchedEffect(callState) {
        if (callState is CallState.Connected) {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (companion != null) {
                // Large avatar on call screen
                CompanionAvatar(
                    emoji = companion.avatarEmoji,
                    accentColor = Color(companion.accentColorHex),
                    avatarAsset = companion.avatarAsset,
                    avatarUrl = companion.avatarUrl,
                    size = 140.dp
                )

                Text(
                    text = companion.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp)
                )

                CallStatusLabel(
                    state = callState,
                    micGranted = micGranted,
                    elapsedSeconds = elapsedSeconds
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            CallActionButton(
                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                selected = isMuted,
                contentDescription = "Mute",
                onClick = {
                    isMuted = !isMuted
                    viewModel.toggleMute(isMuted)
                }
            )
            CallActionButton(
                icon = Icons.Filled.CallEnd,
                selected = true,
                isDanger = true,
                contentDescription = "End call",
                onClick = {
                    viewModel.endCall()
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        com.aicomp.ads.AdManager.showIfReady(activity) { onEndCall() }
                    } else {
                        onEndCall()
                    }
                }
            )
            CallActionButton(
                icon = Icons.Filled.VolumeUp,
                selected = isSpeakerOn,
                contentDescription = "Speaker",
                onClick = {
                    isSpeakerOn = !isSpeakerOn
                    viewModel.toggleSpeaker(isSpeakerOn)
                }
            )
        }
        }

        CoinBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
        )
    }
}

@Composable
private fun CallStatusLabel(state: CallState, micGranted: Boolean, elapsedSeconds: Int) {
    val text = when {
        !micGranted -> "Waiting for microphone permission..."
        state is CallState.Connecting -> "Connecting..."
        state is CallState.Connected && !state.remoteJoined -> "Waiting to connect..."
        state is CallState.Connected && state.remoteJoined -> {
            val m = elapsedSeconds / 60
            val s = elapsedSeconds % 60
            "%d:%02d".format(m, s)
        }
        state is CallState.Failed -> state.reason
        state is CallState.Ended -> "Call ended"
        else -> "Starting..."
    }

    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state is CallState.Connecting) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (state is CallState.Failed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    contentDescription: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when {
        isDanger -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val iconColor = if (isDanger || selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = iconColor)
        }
    }
}
