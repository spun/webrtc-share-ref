package com.spundev.webrtcshare

import androidx.multidex.MultiDexApplication
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class WebRTCShareApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        // Disable offline capabilities Realtime Database
        Firebase.database.setPersistenceEnabled(false)
    }
}