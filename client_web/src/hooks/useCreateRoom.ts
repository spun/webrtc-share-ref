import { useState, useEffect } from 'react';
import { realTimeDatabase } from '../utils/firebase';
import { ref, set, push, child } from "firebase/database";

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
      // Create a new child inside the "rooms" node and get a reference
      const newRoom = push(child(ref(db), 'rooms'));
      // Set the title value
      await set(newRoom.ref, { title: 'title' });
      // Update the roomId value with the child key
      setRoomId(newRoom.key);
    })();
  }, []);

  return roomId;
}

export default useCreateRoom;
