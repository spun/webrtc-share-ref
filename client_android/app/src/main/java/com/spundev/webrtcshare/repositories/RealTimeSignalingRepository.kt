package com.spundev.webrtcshare.repositories

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.spundev.webrtcshare.model.Candidate
import com.spundev.webrtcshare.model.Description
import com.spundev.webrtcshare.utils.MyChildEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

// Message format for the RealtimeDatabase
data class RealTimeDatabaseMessage @JvmOverloads constructor(
    val description: String? = null,
    val candidate: String? = null
)

class RealTimeSignalingRepository(isInitiator: Boolean) : SignalingRepository {

    // Realtime database instance
    private val db = Firebase.database.reference

    // References of the RealtimeDatabase were we can send our message and listen for
    // new messages we want to receive
    val dbMessageFolders = if (isInitiator) {
        Pair("initiatorMessages", "nonInitiatorMessages")
    } else {
        Pair("nonInitiatorMessages", "initiatorMessages")
    }

    val jsonSerializer = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Send a new message to the other end using our database.
     * @param roomId The id of the room that we are using for the signaling
     * @param message Message we want to send to the other end
     */
    override fun sendMessage(roomId: String, message: SignalingMessage) {

        // Before sending the message to the database, we convert it to a json string and populate
        // only the "candidate" or the "description" value of a new "RealTimeDatabaseMessage".
        // This will allow us to use an unique message "queue" for candidates and descriptions
        // that can be easily differentiated when retrieved from the database.
        val dbMessage = when (message) {
            is SignalingMessage.SignalingCandidate -> {
                // Candidate to json string
                val stringCandidate = jsonSerializer.encodeToString(
                    Candidate.serializer(),
                    Candidate(
                        message.candidate.sdp,
                        message.candidate.sdpMid,
                        message.candidate.sdpMLineIndex
                    )
                )
                RealTimeDatabaseMessage(candidate = stringCandidate)
            }
            is SignalingMessage.SignalingDescription -> {
                // Description to json string
                val stringDescription = jsonSerializer.encodeToString(
                    Description.serializer(),
                    Description(
                        message.description.type.canonicalForm(),
                        message.description.description
                    )
                )
                RealTimeDatabaseMessage(description = stringDescription)
            }
        }
        // Add the new message to the list of messages of the database
        val newMessageRef = db.child("rooms/$roomId").child(dbMessageFolders.first).push()
        newMessageRef.setValue(dbMessage)
    }

    /**
     * Receive messages from the database
     * @param roomId The id of the room that we are using for the signaling
     * @return A Flow that will offer the messages received as [SignalingMessage]
     */
    override fun receiveMessagesFlow(roomId: String) = callbackFlow<SignalingMessage> {

        val childEventListener = object : MyChildEventListener() {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot.getValue<RealTimeDatabaseMessage>()?.let { message ->
                    when {
                        // If the description value exists, we are receiving a SessionDescription
                        message.description != null -> {
                            // Json Parse
                            val descriptionObject = jsonSerializer.decodeFromString(
                                Description.serializer(),
                                message.description
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
                                trySend(SignalingMessage.SignalingDescription(description))
                            }
                        }
                        // If the description value exists, we are receiving an IceCandidate
                        message.candidate != null -> {
                            // Json Parse
                            val candidatesObject = jsonSerializer.decodeFromString(
                                Candidate.serializer(),
                                message.candidate
                            )
                            // Candidate recreation
                            val candidate = IceCandidate(
                                candidatesObject.sdpMid,
                                candidatesObject.sdpMLineIndex!!,
                                candidatesObject.candidate
                            )
                            trySend(SignalingMessage.SignalingCandidate(candidate))
                        }
                        else -> { /* Unknown message format. Do nothing */
                        }
                    }
                }
            }
        }
        val messagesListRef = db.child("rooms/$roomId").child(dbMessageFolders.second)
        messagesListRef.addChildEventListener(childEventListener)
        awaitClose { messagesListRef.removeEventListener(childEventListener) }
    }
}