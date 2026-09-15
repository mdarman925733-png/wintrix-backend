package com.aicomp.ui.screens.auth

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicomp.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val navigateNext: Boolean = false
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private var verificationId: String? = null

    // ── Google ────────────────────────────────────────────────────────────────

    fun startGoogleSignIn(
        activity: Activity,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        // Replace WEB_CLIENT_ID with your OAuth 2.0 Web Client ID from Firebase Console
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("90426342126-7tgn7r7dq4ka6glnafmdc6bd5k1j14hn.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(activity, gso)
        launcher.launch(client.signInIntent)
    }

    fun handleGoogleResult(data: Intent?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                AuthRepository.signInWithGoogle(account.idToken!!)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Google sign-in failed"
                )
            }
        }
    }

    // ── Phone OTP ─────────────────────────────────────────────────────────────

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val phone = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"

        AuthRepository.sendOtp(
            phoneNumber = phone,
            activity = activity,
            callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    viewModelScope.launch {
                        try {
                            AuthRepository.signInWithCredential(credential)
                            _uiState.value = _uiState.value.copy(isLoading = false, navigateNext = true)
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                        }
                    }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "OTP bhejne mein dikkat aayi"
                    )
                }

                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    _uiState.value = _uiState.value.copy(isLoading = false, otpSent = true)
                }
            }
        )
    }

    fun verifyOtp(otp: String, onSuccess: () -> Unit) {
        val vId = verificationId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                AuthRepository.verifyOtp(vId, otp)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "OTP galat hai"
                )
            }
        }
    }
}
