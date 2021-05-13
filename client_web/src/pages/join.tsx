import React from 'react';
import { useRouter } from 'next/router';
import { useWebRTC } from '../hooks/useWebRTC';

const Join = () => {
  // Get room id parameter from url /join?roomId=<value>
  const router = useRouter();
  const { roomId } = router.query;

  const [isConnected, messages, sendMessage] = useWebRTC(roomId, false);

  function handleClick() {
    sendMessage('cat');
  }

  const listMessages = messages.map((message, index) => (
    <li key={index}>
      {message}
    </li>
  ));

  return (
    <>
      <h1>Join room</h1>
      <p>
        Room ID:
        {roomId}
      </p>
      <p>
        isConnected:
        {isConnected ? 'TRUE' : 'FALSE'}
      </p>
      <hr />
      <h2>Messages</h2>
      <ul>{listMessages}</ul>
      <hr />
      <button type="button" onClick={() => handleClick()}>Send cat</button>
    </>
  );
};

export default Join;
