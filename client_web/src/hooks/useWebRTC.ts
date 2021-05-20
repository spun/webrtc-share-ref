import { useState, useEffect, useCallback } from 'react';
import {
  MessageType, ChannelMessage, ChannelMessageText, ChannelMessageFile,
} from '../types/FileMessage';

import SignalingServer from '../utils/SignalingServer';

// This is the safe size to support cross-browser exchange of data
// https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/Using_data_channels#concerns_with_large_messages
const DEFAULT_CHUNK_SIZE = 16384;
const MAX_CHUNK_SIZE = 262144;

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

/**
 * Hook to receive a file from a sender channel using WebRTC. The hook will create a channel a
 * prenegotiated channel and it will wait for file chunks until it receives the full file.
 * To know how to create the channel (which id) and the size of the file it is trying to receive,
 * the sender channel first has to send a "details message" using an existing connection. When
 * the receiver gets this message, it will use this hooks funtion to start the transfer.
 * Note: This hook is not responsible of receiving the file data.
 * @param peerConnection A RTCPeerConnection to create the new channel
 * @returns A function that receives the details of the file we are trying to get.
 */
function useFileReceiver(
  peerConnection: RTCPeerConnection,
) {
  const receiveFile = useCallback((fileMetadata : ChannelMessageFile) => {
    // Step 1: Create receiver channel
    const fileReceiverChannel = peerConnection.createDataChannel('file transfer (receiver)', { negotiated: true, id: 5 });
    fileReceiverChannel.binaryType = 'arraybuffer';

    // Step 2: Send the "ready" signal (read useFileSender comments to know why we do this).
    fileReceiverChannel.onopen = () => {
      fileReceiverChannel.send('Receiver ready');
    };

    // Step 3: Start receiving chunks
    let receiveBuffer = [];
    let receivedSize = 0;
    let receivedPercentage = 0;
    let chunk : ArrayBuffer;
    fileReceiverChannel.onmessage = (event) => {
      chunk = event.data;
      // Add chunk to buffer
      receiveBuffer.push(chunk);
      receivedSize += chunk.byteLength;

      // Show percentage
      receivedPercentage = Math.floor((receivedSize * 100) / fileMetadata.content.size);
      console.log(`${receivedPercentage} %`);

      // Keep receiving chunks until we have the entire file
      if (receivedSize === fileMetadata.content.size) {
        // Create complete file blob
        const received = new Blob(receiveBuffer);
        receiveBuffer = [];

        // Trigger the download
        const elem = window.document.createElement('a');
        elem.href = window.URL.createObjectURL(received);
        elem.download = fileMetadata.content.filename;
        // Add to dom
        document.body.appendChild(elem);
        // trigger click event
        elem.click();
        // remove from dom
        document.body.removeChild(elem);

        // File transfer done, close channel
        fileReceiverChannel.close();
      }
    };
    fileReceiverChannel.onerror = (error) => { console.error('Receiver (onerror):', error); };
    fileReceiverChannel.onclose = (event) => { console.log('Receiver (onclose):', event); };
  }, [peerConnection]);

  return [receiveFile];
}

/**
 * Hook to send a file to a receiver channel using WebRTC. The hook will first send the other peer
 * the details of the file (size, filename, etc) and the connection (id of the channel). Note, these
 * details are sent using an existing channel. After sending the details, the hook will create a new
 * channel and, after receiving the "ready" signal from the receiver, it will split the file into
 * chunks and it will send them to the receiver.
 * @param peerConnection A RTCPeerConnection to create the new channel
 * @param sendMessageFunction A function to send the file details to the other peer
 * @returns The function that receives the file we want to send
 */
function useFileSender(
  peerConnection: RTCPeerConnection,
  sendMessageFunction: (message: ChannelMessage) => void,
) : [(f: File) => void] {
  const sendFile = useCallback((file: File) => {
    console.log('run send: ', file);

    // Step 1: Notify other peer that we want to send a file
    const fileNotificationMessage : ChannelMessageFile = {
      type: MessageType.FILE,
      content: {
        filename: file.name,
        size: file.size, // In bytes
        hash: 0, // Hexadecimal
        transferChannelId: 5, // TODO: Use UUID unique for transfer
      },
    };
    sendMessageFunction(fileNotificationMessage);

    // Define the size of the chunks
    const chunkSize = DEFAULT_CHUNK_SIZE;
    // Set a buffer limit. If we reach this limit we will wait until the onbufferedamountlow
    // notifies us that the buffer is now low enough to keep sending the file
    const bufferLimit = chunkSize * 8;
    // This is a signal that it will indicate us that we are waiting for the onbufferedamountlow
    // event after reaching the limit. Since onbufferedamountlow will trigger every time the buffer
    // amount is below the threshold, receiving this event could simply mean that our buffer did
    // not have the time to get filled.
    let haltUntilBufferLow = false;

    let offset = 0;
    let chunk : ArrayBuffer;

    // Step 2: Create sender channel and the file reader
    const fileReader = new FileReader();
    const fileSenderChannel = peerConnection.createDataChannel('file transfer (sender)', { negotiated: true, id: 5 });
    fileSenderChannel.binaryType = 'arraybuffer';
    fileSenderChannel.bufferedAmountLowThreshold = chunkSize;

    // Helper funtion to split the file in chunk of chuckSize using the offset
    const readSlice = (o: number) => {
      const slice = file.slice(offset, o + chunkSize);
      fileReader.readAsArrayBuffer(slice);
    };

    // File reader events
    fileReader.onerror = (error) => console.error('Error reading file:', error);
    fileReader.onabort = (event) => console.log('File reading aborted:', event);

    // Each time a chunk is loaded, we start the transfer
    fileReader.addEventListener('load', ({ target }) => {
      chunk = target.result as ArrayBuffer;
      fileSenderChannel.send(chunk);
      offset += chunk.byteLength;

      const { bufferedAmount } = fileSenderChannel;
      // Pause sending if we reach the high water mark
      if (bufferedAmount < bufferLimit) {
        if (offset < file.size) {
          // Request next chunk
          readSlice(offset);
        } else {
          // File transfer done, close channel
          console.log('Done sending ', offset, file.size, bufferedAmount);
          fileSenderChannel.close();
        }
      } else {
        console.log('STOP sending', bufferedAmount);
        haltUntilBufferLow = true;
      }
    });

    fileSenderChannel.onerror = (error) => { console.error('Sender (onerror):', error); };
    fileSenderChannel.onclose = (event) => { console.log('Sender (onclose):', event); };
    fileSenderChannel.onopen = () => {
      console.log(`Sender (onopen) ${fileSenderChannel.readyState}`);
      // Note: If we start sending the file here (onopen) the receiver might not be ready if we
      // are running in Chrome. Apparently, when using prenegotiated datachannel, chrome can fire
      // "onopen" before the channels are ready.
      // This open issue talks about a bug with prenegotiated datachannels in chrome. In the
      // comments they discuss about how prenegotiated datachannel don't follow the spec.
      //    https://bugs.chromium.org/p/webrtc/issues/detail?id=10727
      // In our case, because we are sending immediately after onopen is receiver, send doesn't
      // throw, but the receiver does not trigger onmmessage and the message is lost. We asume that
      // the receiver is the one that is not ready to receive messages, so we need to wait.
      // To avoid this "send before receiver ready" the sender will wait until it received a "ready"
      // message from the receiver.
      // TODO: Non negotiated channels (peerconnection.ondatachannel) don't have this problem, we
      // could try to replace our negotiated datachannels.
    };
    // Step 4: When ready, send the file chunks
    fileSenderChannel.onmessage = ({ data }) => {
      // We have received a "ready" message from the receiver. The receiver should be now ready to
      // receive the file
      console.log('Sender (onmessage):', data);
      // Kickstart the tranfer by requesting the first split
      readSlice(0);
    };
    fileSenderChannel.onbufferedamountlow = (e) => {
      if (haltUntilBufferLow) {
        console.log('onbufferedamountlow', e);
        haltUntilBufferLow = false;
        // Request next chunk
        readSlice(offset);
      }
    };
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
  // List of messages received and sent
  const [messages, setMessages] = useState<ChannelMessage[]>([]);
  // The signaling server used to kickstart the WebRTC communication
  const [singnalingServer, setSignalingServer] = useState<SignalingServer>(null);
  // The RTCPeerConnection initiated
  const [peerConnection] = usePeerConnection(singnalingServer, isInitiator);
  // The main RTCDataChannel created between our peers
  const [channel, isConnected, lastMessage] = useDataChannel(peerConnection, 'chat', { negotiated: true, id: 0 });

  // Create the signaling server if we have a room id. This will trigger the creation of the rest
  // of components that were waiting for the signaling server (peerConnection, channel).
  useEffect(() => {
    console.log('Main use effect: ', roomId);
    if (roomId === '') return;
    setSignalingServer(new SignalingServer(roomId, isInitiator));
  }, [roomId]);

  // A function that will use the existing channel to send a messages
  const sendMessage = useCallback((message: ChannelMessage) => {
    channel.send(JSON.stringify(message));
    setMessages((prevMessages) => [...prevMessages, message]);
  }, [channel]);

  // Utility hooks to transfer files
  const [sendFile] = useFileSender(peerConnection, sendMessage);
  const [receiveFile] = useFileReceiver(peerConnection);

  // This effect will trigger each time the lastMessage receive from the channel changes. This will
  // keep updated our list of messages.
  useEffect(() => {
    if (lastMessage != null) {
      setMessages((prevMessages) => [...prevMessages, lastMessage]);
      // If the received message is a notification that our peer is trying to send us a file, we
      // call the receiveFile function to start the transfer.
      if (lastMessage.type === MessageType.FILE) {
        receiveFile(lastMessage as ChannelMessageFile);
      }
    }
  }, [lastMessage]);

  // Function offered to the components using this hook to simplify the trasnfer of text messages
  // to the other peer
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

  // Function offered to the components using this hook to simplify the trasnfer of files to
  // the other peer
  const sendFileFunction = (file: File) => {
    sendFile(file);
  };

  return [isConnected, messages, sendTextFunction, sendFileFunction];
}

export default useWebRTC;
