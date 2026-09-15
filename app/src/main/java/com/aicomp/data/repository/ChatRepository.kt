package com.aicomp.data.repository

import android.net.Uri
import com.aicomp.data.model.ChatMessage
import com.aicomp.data.model.Sender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Realtime Database + Storage backend for chat history.
 * Path: chats/{uid}/{companionId}/{messageId}
 *
 * Memory strategy:
 * - Every message (chat OR call transcript) is stored under the same path.
 * - getRecentHistory() fetches the last N messages as a formatted string
 *   that can be injected into Gemini system instructions.
 * - Call transcript messages are stored with sender = AI or USER, same model.
 *   The CallViewModel is responsible for saving call-turn transcripts here
 *   via saveMessage() so both Chat and Call share one history.
 */
object ChatRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // How many recent messages to inject into AI context
    private const val HISTORY_CONTEXT_LIMIT = 30

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonymous sign-in failed")
    }

    private fun messagesRef(uid: String, companionId: String) =
        db.getReference("chats").child(uid).child(companionId)

    /** Live stream of a companion's chat history, ordered by timestamp. */
    fun observeMessages(uid: String, companionId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = messagesRef(uid, companionId).orderByChild("timestamp")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun saveMessage(uid: String, companionId: String, message: ChatMessage) {
        messagesRef(uid, companionId).child(message.id).setValue(message).await()
    }

    /**
     * Returns the last [limit] messages as a formatted context string suitable
     * for injecting into Gemini system instructions.
     *
     * Format:
     *   [Previous conversation]
     *   User: ...
     *   AI: ...
     *
     * Both Chat and Call use the same DB path so this is shared memory.
     */
    suspend fun getRecentHistory(
        uid: String,
        companionId: String,
        limit: Int = HISTORY_CONTEXT_LIMIT
    ): String {
        val snapshot = messagesRef(uid, companionId)
            .orderByChild("timestamp")
            .limitToLast(limit)
            .get()
            .await()

        val messages = snapshot.children
            .mapNotNull { it.getValue(ChatMessage::class.java) }
            .sortedBy { it.timestamp }

        if (messages.isEmpty()) return ""

        val formatted = messages.joinToString("\n") { msg ->
            val label = if (msg.sender == Sender.USER) "User" else "AI"
            "$label: ${msg.text}"
        }
        return "[Previous conversation]\n$formatted"
    }

    /** Uploads a voice note; returns the download URL. */
    suspend fun uploadAudio(uid: String, companionId: String, localFile: File): String {
        val fileName = "${UUID.randomUUID()}.m4a"
        val ref = storage.reference.child("companion_audio/$uid/$companionId/$fileName")
        ref.putFile(Uri.fromFile(localFile)).await()
        return ref.downloadUrl.await().toString()
    }
}
