package com.aicomp.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aicomp.data.repository.CompanionRepository
import com.aicomp.ui.components.ChatBubble
import com.aicomp.ui.components.CoinBadge
import com.aicomp.ui.components.CompanionAvatar
import com.aicomp.ui.components.CompanionGlowHeader
import com.aicomp.ui.components.MessageInputBar
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    companionId: String,
    onCallClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val companion = CompanionRepository.getById(companionId)
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val insufficientCoins by viewModel.insufficientCoins.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(companionId) {
        viewModel.loadHistory(companionId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompanionGlowHeader(height = 160.dp)

        Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(companion?.name ?: "Companion", fontWeight = FontWeight.SemiBold)
                        if (isTyping) {
                            Text(
                                text = "typing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CoinBadge(modifier = Modifier.padding(end = 8.dp))
                    if (companion != null) {
                        CompanionAvatar(
                            emoji = companion.avatarEmoji,
                            accentColor = ComposeColor(companion.accentColorHex),
                            avatarAsset = companion.avatarAsset,
                            avatarUrl = companion.avatarUrl,
                            size = 32.dp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    IconButton(onClick = onCallClick) {
                        Icon(Icons.Filled.Call, contentDescription = "Start call")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeColor.Transparent
                )
            )
        },
        bottomBar = {
            MessageInputBar(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    viewModel.sendMessage(companionId, input)
                    input = ""
                }
            )
        },
        containerColor = ComposeColor.Transparent
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }
        }

        if (insufficientCoins) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissInsufficientCoins() },
                title = { Text("Coins khatam") },
                text = { Text("Message bhejne ke liye coins chahiye. Ad dekh kar coins kamao.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissInsufficientCoins() }) {
                        Text("Theek hai")
                    }
                }
            )
        }
    }
}
