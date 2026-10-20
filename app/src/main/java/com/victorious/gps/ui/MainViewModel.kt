package com.victorious.gps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorious.gps.data.Prefs
import com.victorious.gps.data.Profile
import com.victorious.gps.data.ProfileRepository
import com.victorious.gps.data.ProfileValidationException
import com.victorious.gps.mock.MockLocationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: ProfileRepository,
    val prefs: Prefs,
    val controller: MockLocationController,
) : ViewModel() {
    val profiles: StateFlow<List<Profile>> =
        repo.observeProfiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val message = MutableStateFlow<String?>(null)

    fun save(profile: Profile, onDone: () -> Unit = {}) = viewModelScope.launch {
        try {
            repo.save(profile); message.value = "Saved \"${profile.name.trim()}\""; onDone()
        } catch (e: ProfileValidationException) {
            message.value = e.message
        }
    }

    fun delete(profile: Profile) = viewModelScope.launch { repo.delete(profile) }
}
