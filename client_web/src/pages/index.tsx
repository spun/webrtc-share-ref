import React from 'react';
import Link from 'next/link';

function Home() {
  return (
    <ul>
      <li>
        <Link href="/create.html" as="/create">
          <span>Create room</span>
        </Link>
      </li>
      <li>
        <Link href="/join">
          Join room
        </Link>
      </li>
      <li>
        <Link href="/qr">
          QR example
        </Link>
      </li>
      <li>
        <Link href="/about">
          About Us
        </Link>
      </li>
    </ul>
  );
}

export default Home;
