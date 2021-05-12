import { useState, useEffect, useCallback } from 'react';

import SignalingServer from '../utils/SignalingServer';

const Role = {
  HOST: 0,
  GUEST: 1,
};

function useWebRTC(roomId, isInitiator) : [boolean, string[], ((m: string) => void)] {
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const [channel, setChannel] = useState(null);

  const sendMessageFunction = useCallback((message) => {
    setMessages((prevMessages) => [...prevMessages, message]);
    if (!channel) return;
    channel.send(message);
  }, [channel]);

  useEffect(() => {
    if (!roomId || roomId === '') return;

    const signalingServer = new SignalingServer(roomId, isInitiator);
    // keep track of some negotiation state to prevent races and errors
    let makingOffer = false;
    let ignoreOffer = false;
    const polite = true;

    const configuration = { iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] };
    const peerConnection = new RTCPeerConnection(configuration);

    // send any ice candidates to the other peer
    peerConnection.onicecandidate = ({ candidate }) => {
      signalingServer.sendMessage({ candidate });
    };

    // let the "negotiationneeded" event trigger offer generation
    peerConnection.onnegotiationneeded = async () => {
      if (isInitiator) {
        try {
          makingOffer = true;
          await peerConnection.setLocalDescription(await peerConnection.createOffer());
          signalingServer.sendMessage({ description: peerConnection.localDescription });
        } catch (err) {
          console.error(err);
        }
      }
    };

    const dataChannel = peerConnection.createDataChannel('chat', { negotiated: true, id: 0 });
    dataChannel.onopen = () => {
      setIsConnected(true);
      setChannel(dataChannel);
    };
    dataChannel.onclose = () => setIsConnected(false);
    dataChannel.onmessage = ({ data }) => setMessages((prevMessages) => [...prevMessages, data]);

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
            signalingServer.sendMessage({ description: peerConnection.localDescription });
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

    // eslint-disable-next-line consistent-return
    return () => {
      signalingServer.removeOnMessageListener();
    };
  }, [roomId]);

  return [isConnected, messages, sendMessageFunction];
}

export { useWebRTC, Role as SignalingRole };
