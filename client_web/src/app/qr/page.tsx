"use client"

import useCreateRoom from '../../hooks/useCreateRoom';
import useQrGenerator from '../../hooks/useQrGenerator';

export default function Qr() {
  const roomId = useCreateRoom();
  const [qrData, loading, error] = useQrGenerator(roomId);

  console.log(qrData, loading, error);

  return (
    <>
      <div>
        Create room:
        {roomId}
      </div>
      {
        loading && <p>Generating room QR code...</p>
      }
      {
        Object.keys(error).length !== 0
        && <p>Something went wrong when generating the room QR code</p>
      }
      {
        !loading
        && Object.keys(error).length === 0
        && <img alt="Room QR code" src={qrData} />
      }
    </>
  );
}
