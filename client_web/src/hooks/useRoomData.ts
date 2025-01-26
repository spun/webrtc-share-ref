import { useState, useEffect } from 'react';
import { DataSnapshot, ref, off, onValue } from "firebase/database";

import { realTimeDatabase } from '../utils/firebase';


const db = realTimeDatabase;

/**
 * Test hook that retrieves the title inside the room
 * from the signaling server and listens for updates.
 * @param roomId The room id containing the title
 * @returns The stateful value of the title retrieved
 */
function useRoomData(roomId: string) {
  const [title, setTitle] = useState('');

  useEffect(() => {
    // Update the title value from snapshot
    function handleTitleChange(snapshot: DataSnapshot) {
      console.log(snapshot);
      setTitle(snapshot.val());
    }

    // Connect and listen for changes
    const roomRef = ref(db, `rooms/${roomId}/title`);
    onValue(roomRef, (snapshot: DataSnapshot) => {
      handleTitleChange(snapshot);
    });

    // Disconnect
    return () => { off(roomRef); };
  }, [roomId]);

  return title;
}

export default useRoomData;
