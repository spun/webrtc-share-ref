package com.spundev.webrtcshare.model

import kotlinx.serialization.Serializable

@Serializable
data class Description(val type: String, val sdp: String)