import { useState, useEffect, useCallback } from 'react';

import SignalingServer from '../utils/SignalingServer';

const Role = {
  HOST: 0,
  GUEST: 1,
};

/**
 * Hook that join a room using a signaling server and creates a webRtc connection with any other
 * participant in the room.
 * @param roomId The room id we are using to connect
 * @param isInitiator Our rol in the room. Are we initiating the signaling?
 * @returns The conection state, the list of received messages and a function to send messages
 */
function useWebRTC(
  roomId: string,
  isInitiator: boolean,
): [boolean, string[], ((m: string) => void)] {
  // Connection state
  const [isConnected, setIsConnected] = useState(false);
  // List of messages. Both sent and received
  const [messages, setMessages] = useState<string[]>([]);
  // The channel we are using to communicate
  const [channel, setChannel] = useState<RTCDataChannel | null>(null);

  const sendMessageFunction = useCallback((message: string) => {
    setMessages((prevMessages) => [...prevMessages, message]);
    if (!channel) return;
    channel.send(message);
  }, [channel]);

  useEffect(() => {
    // If the roomId is empty, stop. The roomId could be empty if
    // another hook is in charge of retrieve it.
    if (!roomId || roomId === '') return;

    // Create instance of the signaling server
    const signalingServer = new SignalingServer(roomId, isInitiator);

    // keep track of some negotiation state to prevent races and errors
    let makingOffer = false;
    let ignoreOffer = false;
    const polite = true;

    const configuration = { iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] };
    const peerConnection = new RTCPeerConnection(configuration);

    // send any ice candidates to the other peer
    peerConnection.onicecandidate = ({ candidate }) => {
      if (candidate) {
        signalingServer.sendMessage({ candidate });
      }
    };

    // let the "negotiationneeded" event trigger offer generation
    peerConnection.onnegotiationneeded = async () => {
      if (isInitiator) {
        try {
          makingOffer = true;
          await peerConnection.setLocalDescription(await peerConnection.createOffer());
          if (peerConnection.localDescription) {
            signalingServer.sendMessage({ description: peerConnection.localDescription });
          }
        } catch (err) {
          console.error(err);
        }
      }
    };

    const dataChannel = peerConnection.createDataChannel('chat', { negotiated: true, id: 0 });
    dataChannel.onopen = () => {
      // Notify we are now connected
      setIsConnected(true);
      // Save the channel
      setChannel(dataChannel);
    };
    // Listen for channel close events
    dataChannel.onclose = () => setIsConnected(false);
    // Append any received messages to the list
    dataChannel.onmessage = ({ data }) => setMessages((prevMessages) => [...prevMessages, data]);

    // Start listening the signaling server for any new candidates
    signalingServer.setOnMessageListener(async ({ description, candidate }) => {
      try {
        if (description) {
          const offerCollision = description.type === 'offer'
            && (makingOffer || peerConnection.signalingState !== 'stable');

          ignoreOffer = !polite && offerCollision;
          if (ignoreOffer) {
            return;
          }
          await peerConnection.setRemoteDescription(description); // SRD rolls back as needed

          if (description.type === 'offer') {
            await peerConnection.setLocalDescription(await peerConnection.createAnswer());
            if (peerConnection.localDescription) {
              signalingServer.sendMessage({ description: peerConnection.localDescription });
            }
          }
        } else if (candidate) {
          try {
            await peerConnection.addIceCandidate(candidate);
          } catch (err) {
            if (!ignoreOffer) throw err; // Suppress ignored offer's candidates
          }
        }
      } catch (err) {
        console.error(err);
      }
    });

    // Disconnect
    return () => {
      signalingServer.removeOnMessageListener();
    };
  }, [roomId, isInitiator]);

  return [isConnected, messages, sendMessageFunction];
}

export { useWebRTC, Role as SignalingRole };
