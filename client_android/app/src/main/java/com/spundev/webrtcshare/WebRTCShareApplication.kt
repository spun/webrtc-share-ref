package com.spundev.webrtcshare

import androidx.multidex.MultiDexApplication
import com.google.firebase.Firebase
import com.google.firebase.database.database

class WebRTCShareApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        // Disable offline capabilities Realtime Database
        Firebase.database.setPersistenceEnabled(false)
    }
}