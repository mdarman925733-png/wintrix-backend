package com.aicomp.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Coin balance lives at users/{uid}/coins in Realtime DB — same database
 * AuthRepository already uses for the profile. New users implicitly start
 * at 0 (no node written until they first earn or spend).
 */
object CoinRepository {

    private val db = FirebaseDatabase.getInstance()

    private fun ref(uid: String) = db.getReference("users/$uid/coins")

    /** Live balance — updates automatically as coins are earned/spent anywhere. */
    fun observeCoins(uid: String): Flow<Long> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Long::class.java) ?: 0L)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref(uid).addValueEventListener(listener)
        awaitClose { ref(uid).removeEventListener(listener) }
    }

    suspend fun getCoins(uid: String): Long = suspendCancellableCoroutine { cont ->
        ref(uid).get().addOnSuccessListener { snap ->
            if (cont.isActive) cont.resume(snap.getValue(Long::class.java) ?: 0L) {}
        }.addOnFailureListener {
            if (cont.isActive) cont.resume(0L) {}
        }
    }

    /** Fire-and-forget credit, e.g. after a rewarded ad completes. */
    fun addCoins(uid: String, amount: Long) {
        ref(uid).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Long::class.java) ?: 0L
                currentData.value = current + amount
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }

    /**
     * Atomically deducts [amount] coins only if the balance is sufficient.
     * Returns true if the spend succeeded, false if balance was too low
     * (in which case nothing is deducted).
     */
    suspend fun trySpend(uid: String, amount: Long): Boolean = suspendCancellableCoroutine { cont ->
        var sufficient = false
        ref(uid).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val current = currentData.getValue(Long::class.java) ?: 0L
                if (current < amount) {
                    sufficient = false
                    return Transaction.success(currentData)
                }
                sufficient = true
                currentData.value = current - amount
                return Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (cont.isActive) cont.resume(sufficient && committed && error == null) {}
            }
        })
    }
}
