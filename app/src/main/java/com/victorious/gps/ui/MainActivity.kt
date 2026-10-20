package com.victorious.gps.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                var stage by remember {
                    mutableStateOf(
                        when {
                            !vm.prefs.disclaimerAccepted -> Stage.Disclaimer
                            !vm.prefs.onboardingDone -> Stage.Onboarding
                            else -> Stage.Main
                        }
                    )
                }
                when (stage) {
                    Stage.Disclaimer -> DisclaimerScreen(
                        onAccept = { vm.prefs.disclaimerAccepted = true; stage = Stage.Onboarding },
                        onDecline = { finish() },
                    )
                    Stage.Onboarding -> OnboardingScreen(
                        controller = vm.controller,
                        onDone = { vm.prefs.onboardingDone = true; stage = Stage.Main },
                    )
                    Stage.Main -> MainScreen(vm, onShowSetup = { stage = Stage.Onboarding })
                }
            }
        }
    }

    private enum class Stage { Disclaimer, Onboarding, Main }
}
