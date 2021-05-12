import { useState, useEffect } from 'react';
import QRCode from 'qrcode';

function useQrGenerator(content) {
  const [qrData, setQrData] = useState('');

  useEffect(() => {
    QRCode.toDataURL(content, (err, url) => {
      setQrData(url);
    });
  }, [content]);

  return qrData;
}

export default useQrGenerator;
