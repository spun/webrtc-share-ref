import React from 'react';
import { useRouter } from 'next/router';
import { useWebRTC } from '../hooks/useWebRTC';

import {
  MessageType, ChannelMessage, ChannelMessageText, ChannelMessageFile,
} from '../types/FileMessage';

const Join = () => {
  // Get room id parameter from url /join?roomId=<value>
  const router = useRouter();
  const { roomId } = router.query;

  const [isConnected, messages, sendMessage] = useWebRTC(roomId, false);

  function handleClick() {
    sendMessage('cat');
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
