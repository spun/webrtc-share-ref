"use client"

import Image from 'next/image'
import useCreateRoom from '../../hooks/useCreateRoom';
import useQrGenerator from '../../hooks/useQrGenerator';

const qrWidth = 300

export default function Qr() {
  const roomId = useCreateRoom();
  const [qrData, loading, error] = useQrGenerator(roomId, qrWidth);

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
        && <Image
          src={qrData}
          alt="Room QR code"
          width={qrWidth}
          height={qrWidth} />
      }
    </>
  );
}
