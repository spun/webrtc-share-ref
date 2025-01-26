"use client"

import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import { useWebRTC } from '../../hooks/useWebRTC';

function Join() {
  // Get room id parameter from url /join?roomId=<value>
  const searchParams = useSearchParams();
  const roomId = searchParams.get('roomId') ?? "";

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
}

/**
 * Join should be wrapped in a suspense boundary
 * Read more: https://nextjs.org/docs/messages/missing-suspense-with-csr-bailout
 */
export default function JoinWrapper() {
  return (
    <Suspense>
      <Join />
    </Suspense>
  )
}
