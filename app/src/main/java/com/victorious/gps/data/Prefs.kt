package com.victorious.gps.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Prefs @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("victorgps", Context.MODE_PRIVATE)

    var disclaimerAccepted: Boolean
        get() = sp.getBoolean(KEY_DISCLAIMER, false)
        set(v) = sp.edit().putBoolean(KEY_DISCLAIMER, v).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING, false)
        set(v) = sp.edit().putBoolean(KEY_ONBOARDING, v).apply()

    private companion object {
        const val KEY_DISCLAIMER = "disclaimer_accepted_v1"
        const val KEY_ONBOARDING = "onboarding_done_v1"
    }
}
