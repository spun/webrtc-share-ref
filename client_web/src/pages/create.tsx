import React, { useState, useRef } from 'react';
import Link from 'next/link';

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

function readFile(file: File) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => { resolve(reader.result); };
    reader.onerror = () => { reject(new Error('Error loading file')); };
    reader.onabort = () => { reject(new Error('Operation aborted')); };
    reader.readAsText(file);
  });
}

function RTC() {
  // const roomId = useCreateRoom();
  const [roomIdText, setRoomIdText] = useState('room_001');
  const [roomId, setRoomId] = useState('');
  const [isConnected, messages, sendMessage] = useWebRTC(roomId, true);

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
    const name = file.name ? file.name : 'NOT SUPPORTED';
    const type = file.type ? file.type : 'NOT SUPPORTED';
    const size = file.size ? returnFileSize(file.size) : 'NOT SUPPORTED';
    const content = await readFile(file);
    console.log(`Selected file - ${name} (${type}), file size ${size}.`);
    console.log(`Content: ${content}`);
  }

  const listMessages = messages.map((message, index) => (
    <li key={index}>
      {message}
    </li>
  ));

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
