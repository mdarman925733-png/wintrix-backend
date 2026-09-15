package com.aicomp.data.model

data class AiCompanion(
    val id: String,
    val name: String,
    val avatarAsset: String? = null,
    /** Remote photo URL uploaded via the admin panel (Firebase Storage). Takes
     *  priority over [avatarAsset] when present. */
    val avatarUrl: String? = null,
    val avatarEmoji: String,
    val tagline: String,
    val personality: String,
    val accentColorHex: Long,
    val voice: String = "Leda",
    /** Per-companion system instructions sent to Gemini Live */
    val instructions: String = ""
)
