package com.spundev.webrtcshare.repositories

import com.google.firebase.Firebase
import com.google.firebase.database.ChildEvent
import com.google.firebase.database.childEvents
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber
import javax.inject.Inject

/**
 * Simplification of a WebRTC [SessionDescription].
 * This is used to serialize key data from [SessionDescription] into the JSON string that will be
 * sent to the other client via RealTimeDatabase.
 */
@Serializable
private data class Description(val type: String, val sdp: String)

/**
 * Simplification of a WebRTC [IceCandidate].
 * This is used to serialize key data from [IceCandidate] into the JSON string that will be sent to
 * the other client via RealTimeDatabase.
 */
@Serializable
private data class Candidate(val candidate: String, val sdpMid: String, val sdpMLineIndex: Int)

/**
 * RealtimeDatabase message format.
 * We store a description or a candidate as JSON strings.
 */
// NOTE: This class is referenced by a ProGuard/R8 rule.
//  Make sure the rule is still valid if this is modified.
private data class RealTimeDatabaseMessage(
    val description: String? = null,
    val candidate: String? = null
)

class RealTimeSignalingRepository @Inject constructor() : SignalingRepository {

    // Realtime database instance
    private val db = Firebase.database.reference

    // References of the RealtimeDatabase were we can send our message and listen for
    // new messages we want to receive
    private fun getDestinationFolder(isInitiator: Boolean) =
        if (isInitiator) "initiatorMessages" else "nonInitiatorMessages"

    private fun getOriginFolder(isInitiator: Boolean) =
        if (isInitiator) "nonInitiatorMessages" else "initiatorMessages"

    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    override suspend fun createRoom(): String {
        val newRoomRef = db
            .child("rooms")
            .push()
        return requireNotNull(newRoomRef.key)
    }

    /**
     * Send a new message to the other end using our database.
     * @param roomId The id of the room that we are using for the signaling
     * @param message Message we want to send to the other end
     */
    override fun sendMessage(
        isInitiator: Boolean,
        roomId: String, message: SignalingMessage
    ) {
        Timber.d("SEND: ($roomId) $message")
        // Before sending the message to the database, we convert it to a JSON string and populate
        // only the "candidate" or the "description" value of a new "RealTimeDatabaseMessage".
        // This will allow us to use a unique message "queue" for candidates and descriptions
        // that can be easily differentiated when retrieved from the database.
        val dbMessage = message.toRealTimeDatabaseMessage()
        // Add the new message to the list of messages of the database
        val newMessageRef = db
            .child("rooms/$roomId")
            .child(getDestinationFolder(isInitiator))
            .push()
        newMessageRef.setValue(dbMessage)
    }

    /**
     * Receive messages from the database
     * @param roomId The id of the room that we are using for the signaling
     * @return A Flow that will offer the messages received as [SignalingMessage]
     */
    override fun receiveMessagesFlow(
        isInitiator: Boolean,
        roomId: String
    ): Flow<SignalingMessage> {
        val messagesListRef = db
            .child("rooms/$roomId")
            .child(getOriginFolder(isInitiator))

        return messagesListRef.childEvents
            .filterIsInstance<ChildEvent.Added>()
            .mapNotNull { it.snapshot.getValue<RealTimeDatabaseMessage>()?.toSignalingMessage() }
            .onEach { message -> Timber.d("RECEIVE: ($roomId) $message") }
    }

    /**
     * Utility method to transform a [SignalingMessage] into a [RealTimeDatabaseMessage]
     */
    private fun SignalingMessage.toRealTimeDatabaseMessage(): RealTimeDatabaseMessage {
        return when (this) {
            is SignalingMessage.SignalingCandidate -> {
                // Candidate to JSON string
                val stringCandidate = jsonSerializer.encodeToString(
                    Candidate.serializer(),
                    Candidate(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)
                )
                RealTimeDatabaseMessage(candidate = stringCandidate)
            }

            is SignalingMessage.SignalingDescription -> {
                // Description to JSON string
                val stringDescription = jsonSerializer.encodeToString(
                    serializer = Description.serializer(),
                    value = Description(description.type.canonicalForm(), description.description)
                )
                RealTimeDatabaseMessage(description = stringDescription)
            }
        }
    }

    /**
     * Utility method to transform a [RealTimeDatabaseMessage] into a [SignalingMessage]
     */
    private fun RealTimeDatabaseMessage.toSignalingMessage(): SignalingMessage? {
        return when {
            description != null -> {
                // Json Parse
                val descriptionObject = jsonSerializer.decodeFromString(
                    deserializer = Description.serializer(),
                    string = description
                )
                // Obtain Session Description Type from its string value
                val sessionType = when (descriptionObject.type) {
                    SessionDescription.Type.OFFER.canonicalForm() -> SessionDescription.Type.OFFER
                    SessionDescription.Type.ANSWER.canonicalForm() -> SessionDescription.Type.ANSWER
                    else -> null
                }
                // SessionDescription recreation
                sessionType?.let { type ->
                    val description = SessionDescription(
                        type,
                        descriptionObject.sdp
                    )
                    SignalingMessage.SignalingDescription(description)
                }
            }

            candidate != null -> {
                // Json Parse
                val candidatesObject = jsonSerializer.decodeFromString(
                    deserializer = Candidate.serializer(),
                    string = candidate
                )
                // Candidate recreation
                val candidate = IceCandidate(
                    candidatesObject.sdpMid,
                    candidatesObject.sdpMLineIndex,
                    candidatesObject.candidate
                )
                SignalingMessage.SignalingCandidate(candidate)
            }

            else -> null
        }
    }
}
