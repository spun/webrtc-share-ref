import React from 'react';

import useCreateRoom from '../hooks/useCreateRoom';
import useQrGenerator from '../hooks/useQrGenerator';

function Create() {
  const roomId = useCreateRoom();
  const qrData = useQrGenerator(roomId);
  return (
    <>
      <div>
        Create room:
        {roomId}
      </div>
      <img alt="Room QR code" src={qrData} />
    </>
  );
}

export default Create;
