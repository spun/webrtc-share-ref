import React from 'react';
import Link from 'next/link';

import useCreateRoom from '../hooks/useCreateRoom';
import { useWebRTC } from '../hooks/useWebRTC';

function RTC() {
  const roomId = useCreateRoom();
  const [isConnected, messages, sendMessage] = useWebRTC(roomId, true);

  function handleClick() {
    sendMessage('dog');
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
      <Link href={{ pathname: '/join', query: { roomId } }}>
        Room link
      </Link>
      <p>
        isConnected:
        {isConnected ? 'TRUE' : 'FALSE'}
      </p>
      <hr />
      <h2>Messages</h2>
      <ul>{listMessages}</ul>
      <hr />
      <button type="button" onClick={() => handleClick()}>Send dog</button>
    </>
  );
}

export default RTC;
