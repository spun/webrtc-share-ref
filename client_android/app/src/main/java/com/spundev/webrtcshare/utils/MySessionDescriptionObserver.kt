package com.spundev.webrtcshare.utils

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

abstract class MySessionDescriptionObserver : SdpObserver {

    override fun onSetFailure(p0: String?) {
        Log.d(TAG, "onSetFailure: $p0")
    }

    override fun onSetSuccess() {
        Log.d(TAG, "onSetSuccess: ")
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d(TAG, "onCreateSuccess: $p0")
    }

    override fun onCreateFailure(p0: String?) {
        Log.d(TAG, "onCreateFailure: $p0")
    }

    companion object {
        private const val TAG = "MySessionDescriptionObs"
    }
}