package com.aicomp.data.model

enum class Sender { USER, AI }

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val sender: Sender = Sender.USER,
    val timestamp: Long = System.currentTimeMillis()
)
