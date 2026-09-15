package com.aicomp.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val uid: String? get() = auth.currentUser?.uid

    /** True if user is logged in AND has completed profile setup */
    suspend fun isProfileComplete(): Boolean {
        val u = auth.currentUser ?: return false
        val snap = db.getReference("users/${u.uid}/profile").get().await()
        val name = snap.child("name").getValue(String::class.java)
        return !name.isNullOrBlank()
    }

    // ── Google Sign-In ────────────────────────────────────────────────────────

    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: error("Google sign-in returned null user")
    }

    // ── Phone OTP ─────────────────────────────────────────────────────────────

    fun sendOtp(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyOtp(verificationId: String, otp: String): FirebaseUser {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        return signInWithCredential(credential)
    }

    suspend fun signInWithCredential(credential: PhoneAuthCredential): FirebaseUser {
        val result = auth.signInWithCredential(credential).await()
        return result.user ?: error("Phone sign-in returned null user")
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun saveProfile(name: String, photoUri: Uri?) {
        val u = auth.currentUser ?: error("Not signed in")
        var photoUrl: String? = null

        if (photoUri != null) {
            val ref = storage.reference.child("profile_photos/${u.uid}.jpg")
            ref.putFile(photoUri).await()
            photoUrl = ref.downloadUrl.await().toString()
        }

        // Save to Realtime DB
        val profile = mutableMapOf<String, Any>("name" to name)
        if (photoUrl != null) profile["photoUrl"] = photoUrl

        db.getReference("users/${u.uid}/profile").updateChildren(profile).await()

        // Also update Firebase Auth display name
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply { if (photoUrl != null) setPhotoUri(Uri.parse(photoUrl)) }
            .build()
        u.updateProfile(profileUpdates).await()
    }

    suspend fun getProfile(): Map<String, String?> {
        val u = auth.currentUser ?: return emptyMap()
        val snap = db.getReference("users/${u.uid}/profile").get().await()
        return mapOf(
            "name" to snap.child("name").getValue(String::class.java),
            "photoUrl" to snap.child("photoUrl").getValue(String::class.java)
        )
    }

    fun signOut() = auth.signOut()
}
