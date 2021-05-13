import { useState, useEffect } from 'react';
import { realTimeDatabase } from '../utils/firebase';

const db = realTimeDatabase;

/**
 * Creates new room in the signaling server and returns the roomId
 * @returns The id of the newly created room.
 */
function useCreateRoom() {
  // Holds the created roomId and the function to update it
  const [roomId, setRoomId] = useState('');

  useEffect(() => {
    (async () => {
      // Create a new child inside the "rooms" node
      const roomRef = db.ref('rooms').push();
      // Retrieve the id of the generated child
      const roomKey = roomRef.key;
      // Set the title value
      await roomRef.set({ title: 'title' });
      // Update the roomId value with the child key
      setRoomId(roomKey);
    })();
  }, []);

  return roomId;
}

export default useCreateRoom;
