import { useState, useEffect, useCallback } from 'react';
import {
  MessageType, ChannelMessage, ChannelMessageText, ChannelMessageFile,
} from '../types/FileMessage';

import SignalingServer from '../utils/SignalingServer';

const Role = {
  HOST: 0,
  GUEST: 1,
};

function usePeerConnection(signalingServer: SignalingServer, isInitiator: boolean) {
  // Peer connection state holder
  const [peerConnection, setPeerConnection] = useState<RTCPeerConnection>(null);

  // Run if we have signaling server
  useEffect(() => {
    console.log('From usePeerConnection:', signalingServer);
    if (signalingServer == null) return;
    // Create a new RTCPeerConnection
    const configuration = { iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] };
    const newPeerConnection = new RTCPeerConnection(configuration);
    setPeerConnection(newPeerConnection);
  }, [signalingServer]);

  // Run if we have a peer connection
  useEffect(() => {
    if (peerConnection == null) return;

    // keep track of some negotiation state to prevent races and errors
    let makingOffer = false;
    let ignoreOffer = false;
    const polite = true;

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

    peerConnection.ondatachannel = () => {
      console.log('ONDATACHANNEL');
    };

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
    // Disconnect
    // eslint-disable-next-line consistent-return
    return () => {
      signalingServer.removeOnMessageListener();
    };
  }, [peerConnection]);

  return [peerConnection];
}

function useDataChannel(
  peerConnection: RTCPeerConnection,
  label: string,
  options?: RTCDataChannelInit,
) : [RTCDataChannel, boolean, ChannelMessage] {
  const [isConnected, setIsConnected] = useState(false);
  const [channel, setChannel] = useState<RTCDataChannel>(null);
  const [message, setMessage] = useState<ChannelMessage>(null);

  useEffect(() => {
    console.log('From useChannel: ', peerConnection);

    if (peerConnection == null) return;
    const dataChannel = peerConnection.createDataChannel(label, options);
    dataChannel.onopen = () => {
    // Save the channel
      setChannel(dataChannel);
      // Notify we are now connected
      setIsConnected(true);
    };
    dataChannel.onclose = () => {
    // Remove the channel
      setChannel(null);
      // Notify we are not connected
      setIsConnected(false);
    };
    dataChannel.onmessage = ({ data }) => {
      const receivedMessage : ChannelMessage = JSON.parse(data);
      setMessage(receivedMessage);
    };

    // eslint-disable-next-line consistent-return
    return () => {
      dataChannel.close();
    };
  }, [peerConnection]);

  return [channel, isConnected, message];
}

function useFileReceiver(
  peerConnection: RTCPeerConnection,
) {
  const receiveFile = useCallback((fileMetadata : ChannelMessageFile) => {
    console.log('run receive');

    // Create receiver channel
    const fileSenderChannel = peerConnection.createDataChannel('file transfer (receiver)', { negotiated: true, id: 5 });
    fileSenderChannel.onopen = () => {
      console.log(`Receiver (onopen) ${fileSenderChannel.readyState}`);
      fileSenderChannel.send('Receiver ready');
    };
    fileSenderChannel.onmessage = ({ data }) => { console.log('Receiver (onmessage):', data); };
    fileSenderChannel.onerror = (error) => { console.error('Receiver (onerror):', error); };
    fileSenderChannel.onclose = (event) => { console.log('Receiver (onclose):', event); };
  }, [peerConnection]);

  return [receiveFile];
}

function useFileSender(
  peerConnection: RTCPeerConnection,
  sendMessageFunction: (message: ChannelMessage) => void,
) : [(f: File) => void] {
  const sendFile = useCallback((file: File) => {
    console.log('run send: ', file);

    // Create sender channel
    const fileSenderChannel = peerConnection.createDataChannel('file transfer (sender)', { negotiated: true, id: 5 });
    fileSenderChannel.onopen = () => {
      console.log(`Sender (onopen) ${fileSenderChannel.readyState}`);
      fileSenderChannel.send('Sender ready');
      for (let i = 0; i < 10; i += 1) {
        console.log(`Sender (onopen): Send ${i}`);
        fileSenderChannel.send(`Sender fake file chunk ${i}`);
      }
    };
    fileSenderChannel.onmessage = ({ data }) => { console.log('Sender (onmessage):', data); };
    fileSenderChannel.onerror = (error) => { console.error('Sender (onerror):', error); };
    fileSenderChannel.onclose = (event) => { console.log('Sender (onclose):', event); };

    // Notify other peer that we want to send a file
    const fileNotificationMessage : ChannelMessageFile = {
      type: MessageType.FILE,
      content: {
        filename: file.name,
        size: file.size, // In bytes
        hash: 0, // Hexadecimal
        transferChannelId: fileSenderChannel.id, // TODO: Use UUID unique for transfer
      },
    };
    sendMessageFunction(fileNotificationMessage);
  }, [peerConnection, sendMessageFunction]);

  return [sendFile];
}

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
) : [boolean, ChannelMessage[], ((m: string) => void), ((f: File) => void)] {
  const [messages, setMessages] = useState<ChannelMessage[]>([]);

  const [singnalingServer, setSignalingServer] = useState<SignalingServer>(null);
  const [peerConnection] = usePeerConnection(singnalingServer, isInitiator);
  const [channel, isConnected, lastMessage] = useDataChannel(peerConnection, 'chat', { negotiated: true, id: 0 });

  useEffect(() => {
    console.log('Main use effect: ', roomId);
    if (roomId === '') return;

    setSignalingServer(new SignalingServer(roomId, isInitiator));
  }, [roomId]);

  const sendMessage = useCallback((message: ChannelMessage) => {
    channel.send(JSON.stringify(message));
    setMessages((prevMessages) => [...prevMessages, message]);
  }, [channel]);

  const [sendFile] = useFileSender(peerConnection, sendMessage);
  const [receiveFile] = useFileReceiver(peerConnection);

  useEffect(() => {
    if (lastMessage != null) {
      setMessages((prevMessages) => [...prevMessages, lastMessage]);
      if (lastMessage.type === MessageType.FILE) {
        receiveFile(lastMessage as ChannelMessageFile);
      }
    }
  }, [lastMessage]);

  const sendTextFunction = useCallback((text) => {
    if (!channel) return;
    const textMessage : ChannelMessageText = {
      type: MessageType.TEXT,
      content: {
        timestamp: Date.now(),
        value: text,
      },
    };
    sendMessage(textMessage);
  }, [channel]);

  const sendFileFunction = (file: File) => {
    sendFile(file);
  };

  return [isConnected, messages, sendTextFunction, sendFileFunction];
}

export { useWebRTC, Role as SignalingRole };
