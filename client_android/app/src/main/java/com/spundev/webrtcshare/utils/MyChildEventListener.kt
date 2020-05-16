package com.spundev.webrtcshare.utils

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

abstract class MyChildEventListener : ChildEventListener {
    override fun onCancelled(p0: DatabaseError) {
        Log.d(TAG, "onCancelled: ")
    }

    override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        Log.d(TAG, "onChildMoved: ")
    }

    override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        Log.d(TAG, "onChildChanged: ")
    }

    override fun onChildAdded(p0: DataSnapshot, p1: String?) {
        Log.d(TAG, "onChildAdded: ")
    }

    override fun onChildRemoved(p0: DataSnapshot) {
        Log.d(TAG, "onChildRemoved: ")
    }

    companion object {
        private const val TAG = "MyChildEventListener"
    }
}