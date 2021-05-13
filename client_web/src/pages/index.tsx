import React from 'react';
import Link from 'next/link';

const routes = [{
  name: 'Create room',
  path: '/create',
}, {
  name: 'Join room',
  path: '/join',
}, {
  name: 'QR example',
  path: '/qr',
}, {
  name: 'About Us',
  path: '/about',
}];

function Home() {
  return (
    <ul>
      {
        routes.map((item) => (
          <li>
            <Link href={item.path} passHref>
              <a href="replace">{item.name}</a>
            </Link>
          </li>
        ))
      }
    </ul>
  );
}

export default Home;
