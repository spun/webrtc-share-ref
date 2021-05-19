import React, { useState, useRef } from 'react';
import Link from 'next/link';

import {
  MessageType, ChannelMessage, ChannelMessageText, ChannelMessageFile,
} from '../types/FileMessage';

// Components
import FileButton from '../components/fileLoadForm/fileLoadForm';

// import useCreateRoom from '../hooks/useCreateRoom';
import useWebRTC from '../hooks/useWebRTC';

function RTC() {
  // const roomId = useCreateRoom();
  const [roomIdText, setRoomIdText] = useState('room_001');
  const [roomId, setRoomId] = useState('');
  const [isConnected, messages, sendMessage, sendFile] = useWebRTC(roomId, true);

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
      <FileButton onFileLoaded={sendFile} />
      <hr />
      <h2>Messages</h2>
      <ul>{listMessages}</ul>
      <hr />
      <button type="button" onClick={() => handleClick()}>Send dog</button>
    </>
  );
}

export default RTC;
