import { useState, useEffect } from 'react';
import { realTimeDatabase } from '../utils/firebase';

const db = realTimeDatabase;

function useRoomData(roomId) {
  const [title, setTitle] = useState('');

  useEffect(() => {
    function handleTitleChange(snapshot) {
      console.log(snapshot);
      setTitle(snapshot.val());
    }

    const ref = db.ref(`rooms/${roomId}/title`);
    ref.on('value', (snapshot) => {
      handleTitleChange(snapshot);
    });

    return () => { ref.off(); };
  }, []);

  return title;
}

export default useRoomData;
