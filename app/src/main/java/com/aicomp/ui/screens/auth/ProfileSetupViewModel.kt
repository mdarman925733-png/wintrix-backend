package com.aicomp.ui.screens.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicomp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileSetupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
)

class ProfileSetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState

    fun saveProfile(name: String, photoUri: Uri?, onSuccess: () -> Unit) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Naam daalo pehle")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                AuthRepository.saveProfile(name, photoUri)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Profile save nahi hua"
                )
            }
        }
    }
}
