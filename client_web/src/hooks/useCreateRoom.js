import { useState, useEffect } from 'react';
import realTimeDatabase from '../utils/firebase';

const db = realTimeDatabase;

function useCreateRoom() {
  const [roomId, setRoomId] = useState('');

  useEffect(() => {
    (async () => {
      const roomRef = db.ref('rooms').push();
      const roomKey = roomRef.key;
      await roomRef.set({ title: 'title' });
      setRoomId(roomKey);
    })();
  }, []);

  return roomId;
}

export default useCreateRoom;
