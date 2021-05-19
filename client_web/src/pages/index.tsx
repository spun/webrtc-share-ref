import React from 'react';
import Link from 'next/link';

const routes = [{
  id: 0,
  name: 'Create room',
  path: '/create',
}, {
  id: 1,
  name: 'Join room',
  path: '/join',
}, {
  id: 2,
  name: 'QR example',
  path: '/qr',
}, {
  id: 3,
  name: 'About Us',
  path: '/about',
}];

function Home() {
  return (
    <ul>
      {
        routes.map((item) => (
          <li key={item.id}>
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
