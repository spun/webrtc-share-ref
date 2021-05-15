import { useState, useEffect } from 'react';
import { firebase, realTimeDatabase } from '../utils/firebase';

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
    function handleTitleChange(snapshot: firebase.database.DataSnapshot) {
      console.log(snapshot);
      setTitle(snapshot.val());
    }

    // Connect and listen for changes
    const ref = db.ref(`rooms/${roomId}/title`);
    ref.on('value', (snapshot) => {
      handleTitleChange(snapshot);
    });

    // Disconnect
    return () => { ref.off(); };
  }, []);

  return title;
}

export default useRoomData;
