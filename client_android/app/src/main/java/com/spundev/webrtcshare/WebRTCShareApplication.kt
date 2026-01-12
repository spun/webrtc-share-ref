package com.spundev.webrtcshare

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.database.database
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WebRTCShareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Disable offline capabilities Realtime Database
        Firebase.database.setPersistenceEnabled(false)
    }
}