import React, { useState, useRef } from 'react';
import Link from 'next/link';

import {
  MessageType, ChannelMessage, ChannelMessageText, ChannelMessageFile,
} from '../types/FileMessage';

// import useCreateRoom from '../hooks/useCreateRoom';
import { useWebRTC } from '../hooks/useWebRTC';

function returnFileSize(number: number) : string {
  if (number < 1024) {
    return `${number}bytes`;
  } if (number >= 1024 && number < 1048576) {
    return `${(number / 1024).toFixed(1)}KB`;
  } if (number >= 1048576) {
    return `${(number / 1048576).toFixed(1)}MB`;
  }
  return 'unknown';
}

function readFile(file: File): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => { resolve(reader.result as ArrayBuffer); };
    reader.onerror = () => { reject(new Error('Error loading file')); };
    reader.onabort = () => { reject(new Error('Operation aborted')); };
    reader.readAsArrayBuffer(file);
  });
}

function RTC() {
  // const roomId = useCreateRoom();
  const [roomIdText, setRoomIdText] = useState('room_001');
  const [roomId, setRoomId] = useState('');
  const [isConnected, messages, sendMessage, sendFile] = useWebRTC(roomId, true);

  const fileInput = useRef(null);

  function handleClick() {
    sendMessage('dog');
  }

  function handleRoomIdInputChange(event: React.ChangeEvent) {
    const target = event.target as HTMLInputElement;
    setRoomIdText(target.value);
  }

  function handleRoomIdInputButton() {
    setRoomId(roomIdText);
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const file = fileInput.current.files[0];
    sendFile(file);
    /*
    const name = file.name ? file.name : 'NOT SUPPORTED';
    const type = file.type ? file.type : 'NOT SUPPORTED';
    const size = file.size ? returnFileSize(file.size) : 'NOT SUPPORTED';
    console.log(`Selected file - ${name} (${type}), file size ${size}.`);

    const contentArray = await readFile(file);
    // const decoder = new TextDecoder();
    // const contentText = decoder.decode(contentArray);
    // console.log(`Content: ${contentText}`);

    const hashArrayBuffer = await crypto.subtle.digest('SHA-256', contentArray); // hash the message
    const hashArray = Array.from(new Uint8Array(hashArrayBuffer));
    const hashHex = hashArray.map((b) => b.toString(16).padStart(2, '0')).join(''); // convert bytes to hex string
    console.log(`Hash: ${hashHex}`);
    */
  }

  const listMessages = messages.reduce((result: JSX.Element[], message, index) => {
    switch (message.type) {
      case MessageType.TEXT: {
        const textMessage = message as ChannelMessageText;
        result.push(<li key={index}>{textMessage.content.value}</li>);
        break;
      }
      case MessageType.FILE: {
        const fileMessage = message as ChannelMessageFile;
        console.log('fileMessage', fileMessage);
        result.push(<li key={index}>{fileMessage.content.filename}</li>);
        break;
      }
      default:
        break;
    }
    return result;
  }, []);

  return (
    <>
      <h1>Create room</h1>
      <p>
        Room ID:
        {roomId}
      </p>

      { roomId === ''
        && (
        <div>
          <p>
            Use room with id (dev option to avoid room creation):
          </p>
          <p>
            <input value={roomIdText} onChange={handleRoomIdInputChange} />
            <button type="button" onClick={handleRoomIdInputButton}>Use room with id</button>
          </p>
        </div>
        )}

      <Link href={{ pathname: '/join', query: { roomId } }}>
        Room link
      </Link>
      <p>
        isConnected:
        {isConnected ? 'TRUE' : 'FALSE'}
      </p>
      <hr />
      <h2>File share</h2>
      <form onSubmit={handleSubmit}>
        <input type="file" ref={fileInput} />
        <button type="submit">Load file</button>
      </form>
      <hr />
      <h2>Messages</h2>
      <ul>{listMessages}</ul>
      <hr />
      <button type="button" onClick={() => handleClick()}>Send dog</button>
    </>
  );
}

export default RTC;
