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


export default function Home() {
  return (
    <ul>
      {
        routes.map((item) => (
          <li key={item.name}>
            <Link href={item.path} passHref>{item.name}</Link>
          </li>
        ))
      }
    </ul>
  );
}
