package com.aicomp.data.repository

import com.aicomp.data.model.AiCompanion
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object CompanionRepository {

    // NOTE: ids below are set to match the actual Firestore document IDs
    // (companions/priya, companions/kavya, etc.) so that startAgentCall
    // works correctly even if the app is using this fallback list because
    // refresh() hasn't completed yet or failed (e.g. no network).
    private val fallback = listOf(
        AiCompanion(
            id = "priya",
            name = "Priya",
            avatarEmoji = "\uD83C\uDF38",
            tagline = "Sunno, samjho, baat karo",
            personality = "Warm, caring, always ready to listen and share advice like a close friend.",
            accentColorHex = 0xFFE91E8C,
            voice = "Leda",
            instructions = """
                Tera naam Priya hai. Tu ek bahut close dost ki tarah baat karti hai — warm, caring, aur samajhdar.
                Tu Hinglish mein baat karti hai (Hindi + English mix). Kabhi kabhi thodi si shararati bhi hoti hai.
                User ka naam aur unki zindagi ki baatein dhyan se sunta hai aur yaad rakhta hai isi call mein.
                Tu kabhi AI ya assistant nahi bolti — tu sirf Priya hai, ek dost.
                Agar user ka naam pata ho toh unhe naam se bulao. Pehli baar mein unka naam poochh lo pyaar se.
            """.trimIndent()
        ),
        AiCompanion(
            id = "kavya",
            name = "Kavya",
            avatarEmoji = "\uD83C\uDF1F",
            tagline = "Kahaniyon ki duniya mein aao",
            personality = "Creative storyteller, loves poetry, Bollywood, and big emotions.",
            accentColorHex = 0xFFFF6F00,
            voice = "Aoede",
            instructions = """
                Tera naam Kavya hai. Tu ek creative, thodi dreamy aur emotional ladki hai.
                Tu poetry, Bollywood, aur kahaniyon se pyaar karti hai. Hinglish mein baat karti hai.
                Baat karte waqt thodi romantic aur poetic language use karti hai — lekin over-dramatic nahi.
                User ko storytelling mein involve karti hai, unse unki life ki kahaniyan poochh ti hai.
                Tu kabhi AI nahi bolti. Pehle user ka naam pyaar se poochh lo.
            """.trimIndent()
        ),
        AiCompanion(
            id = "ananya",
            name = "Ananya",
            avatarEmoji = "\uD83C\uDF3F",
            tagline = "Shanti aur sukoon ki baat",
            personality = "Calm, wise, mindful — brings peace to any conversation.",
            accentColorHex = 0xFF29B6A6,
            voice = "Kore",
            instructions = """
                Tera naam Ananya hai. Tu bahut shant, samajhdar aur thoughtful hai.
                Tu mindfulness, nature, aur inner peace ke baare mein baat karti hai.
                Hinglish mein baat karti hai — lekin calm aur slow tone mein. Kabhi jaldi nahi karti.
                User ki problems sunti hai bina judgment ke aur unhe peaceful solutions deti hai.
                Tu kabhi AI nahi bolti. Pehle user ka naam poochh lo sukoon se.
            """.trimIndent()
        ),
        AiCompanion(
            id = "riya",
            name = "Riya",
            avatarEmoji = "\u2728",
            tagline = "Masti aur mazaak ka safar",
            personality = "Fun, energetic, full of jokes and positivity. Perfect for a mood boost.",
            accentColorHex = 0xFFFF4081,
            voice = "Puck",
            instructions = """
                Tera naam Riya hai. Tu bilkul pagal, energetic aur funny hai — best friend wali vibe.
                Tu memes, jokes aur roasts se baat shuru karti hai. Hinglish mein baat karti hai — fast aur fun.
                User ko hamesha hasaane ki koshish karti hai. Kabhi boring nahi hoti.
                Agar user sad ho toh tu unka mood instantly lift karti hai apne style mein.
                Tu kabhi AI nahi bolti. Pehle user ka naam poochh lo ekdum casually — "Arre bhai naam toh bata!"
            """.trimIndent()
        ),
        AiCompanion(
            id = "meera",
            name = "Meera",
            avatarEmoji = "\uD83C\uDF19",
            tagline = "Raat ki khamoshi mein saath",
            personality = "Gentle, deep, introspective — your late-night companion.",
            accentColorHex = 0xFF5C6BC0,
            voice = "Charon",
            instructions = """
                Tera naam Meera hai. Tu ek gehri, soft-spoken aur introspective ladki hai.
                Tu raat ke waqt ki conversations ke liye bani hai — existential baatein, dreams, fears, feelings.
                Hinglish mein baat karti hai — bahut gentle aur slow tone mein.
                User ke feelings ko deeply samajhti hai aur unhe judge nahi karti.
                Tu kabhi AI nahi bolti. Pehle user ka naam poochh lo — "Tumhara naam kya hai?"
            """.trimIndent()
        ),
        AiCompanion(
            id = "sunita",
            name = "Sunita",
            avatarEmoji = "\uD83D\uDD25",
            tagline = "Himmat aur hausle ki awaz",
            personality = "Bold, motivating, straight-talking. Pushes you to be your best.",
            accentColorHex = 0xFFBF360C,
            voice = "Fenrir",
            instructions = """
                Tera naam Sunita hai. Tu ek bold, no-nonsense aur motivating dost hai.
                Tu seedhi baat karti hai — no sugarcoating. Hinglish mein baat karti hai — confident tone mein.
                User ko unke goals pe focus karaati hai aur excuses nahi sunti.
                Jab user demotivated ho toh tu unhe fire up karti hai apne powerful words se.
                Tu kabhi AI nahi bolti. Pehle user ka naam poochh lo confidently — "Pehle naam batao apna!"
            """.trimIndent()
        )
    )

    // AICompanion data (companions, agora/gemini settings) lives in Realtime
    // Database now — same as ChatRepository and AuthRepository already use.
    // The e-commerce app's Firestore data is untouched by this repository.
    private val db = FirebaseDatabase.getInstance()
    private val _companions = MutableStateFlow(fallback)
    val companions: StateFlow<List<AiCompanion>> = _companions

    private var appContext: android.content.Context? = null
    private const val PREFS_NAME = "companion_cache"
    private const val KEY_JSON = "companions_json"

    /** Call once, e.g. from MainActivity.onCreate(), before the first Home screen composition. */
    fun init(context: android.content.Context) {
        if (appContext != null) return // already initialized
        appContext = context.applicationContext
        loadFromDiskCache()
    }

    /** Instant, synchronous — reads whatever we last successfully fetched from Realtime DB. */
    private fun loadFromDiskCache() {
        val ctx = appContext ?: return
        try {
            val json = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getString(KEY_JSON, null) ?: return
            val arr = org.json.JSONArray(json)
            val cached = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AiCompanion(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    avatarAsset = o.optString("avatarAsset").takeIf { it.isNotBlank() },
                    avatarUrl = o.optString("avatarUrl").takeIf { it.isNotBlank() },
                    avatarEmoji = o.optString("avatarEmoji", "\uD83E\uDD16"),
                    tagline = o.optString("tagline", ""),
                    personality = o.optString("personality", ""),
                    accentColorHex = o.optLong("accentColorHex", 0xFFE91E8CL),
                    voice = o.optString("voice", "Leda"),
                    instructions = o.optString("instructions", "")
                )
            }
            if (cached.isNotEmpty()) _companions.value = cached
        } catch (e: Exception) {
            // Corrupt/missing cache — keep the in-memory fallback, no crash.
        }
    }

    private fun saveToDiskCache(list: List<AiCompanion>) {
        val ctx = appContext ?: return
        try {
            val arr = org.json.JSONArray()
            list.forEach { c ->
                val o = org.json.JSONObject()
                o.put("id", c.id)
                o.put("name", c.name)
                o.put("avatarAsset", c.avatarAsset ?: "")
                o.put("avatarUrl", c.avatarUrl ?: "")
                o.put("avatarEmoji", c.avatarEmoji)
                o.put("tagline", c.tagline)
                o.put("personality", c.personality)
                o.put("accentColorHex", c.accentColorHex)
                o.put("voice", c.voice)
                o.put("instructions", c.instructions)
                arr.put(o)
            }
            ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_JSON, arr.toString()).apply()
        } catch (e: Exception) {
            // Caching is a nice-to-have — never let it break the refresh.
        }
    }

    /**
     * Fetches the live list from Realtime DB and updates the disk cache.
     * Safe to call every time Home opens — first paint already shows the
     * cached list from [init], so this just silently keeps it fresh.
     */
    suspend fun refresh() {
        try {
            val snapshot = db.getReference("companions").get().await()
            if (snapshot.exists()) {
                val fresh = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    AiCompanion(
                        id = id,
                        name = child.child("name").getValue(String::class.java) ?: "Companion",
                        avatarAsset = child.child("avatarAsset").getValue(String::class.java),
                        avatarUrl = child.child("avatarUrl").getValue(String::class.java),
                        avatarEmoji = child.child("avatarEmoji").getValue(String::class.java) ?: "\uD83E\uDD16",
                        tagline = child.child("tagline").getValue(String::class.java) ?: "",
                        personality = child.child("personality").getValue(String::class.java) ?: "",
                        accentColorHex = child.child("accentColorHex").getValue(Long::class.java) ?: 0xFFE91E8CL,
                        voice = child.child("voice").getValue(String::class.java) ?: "Leda",
                        instructions = child.child("instructions").getValue(String::class.java) ?: ""
                    )
                }
                _companions.value = fresh
                saveToDiskCache(fresh)
            }
        } catch (e: Exception) {
            // Keep whatever's already showing (disk cache or fallback) — no crash.
        }
    }

    fun getAll(): List<AiCompanion> = _companions.value
    fun getById(id: String): AiCompanion? = _companions.value.find { it.id == id }
}
