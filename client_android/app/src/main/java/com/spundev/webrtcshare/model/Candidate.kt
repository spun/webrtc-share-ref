package com.spundev.webrtcshare.model

import kotlinx.serialization.Serializable

@Serializable
data class Candidate(val candidate: String, val sdpMid: String, val sdpMLineIndex: Int)
