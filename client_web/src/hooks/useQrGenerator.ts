import { useState, useEffect } from 'react';
import QRCode from 'qrcode';

/**
 * Generate a qr image in base64
 * @param content The content of the qr
 * @returns A qr image in base64
 */
function useQrGenerator(content: string) {
  // Holds the data of the image we are going to generate
  const [qrData, setQrData] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState({});

  useEffect(() => {
    async function generateQrCode() {
      // Reset error and loading state
      setLoading(true);
      setError({});

      // If we are getting and empty content, it could be that the content
      // is being generated in another hook and we should wait
      if (content === '') return;

      // Generate code
      try {
        const url = await QRCode.toDataURL(content);
        setQrData(url);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    }
    generateQrCode();
  }, [content]);

  return [qrData, loading, error];
}

export default useQrGenerator;
