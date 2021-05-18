import React, { useRef } from 'react';

const FileLoadForm = ({ onFileLoaded } : FileLoadFormProps) => {
  const fileInput = useRef(null);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (fileInput.current.files.length === 1) {
      const file = fileInput.current.files[0];
      onFileLoaded(file);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <input type="file" ref={fileInput} />
      <button type="submit">Load file</button>
    </form>
  );
};

type FileLoadFormProps = {
  onFileLoaded: Function
};

export default FileLoadForm;
