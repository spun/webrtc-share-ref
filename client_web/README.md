# Web Client

## Configure firebase

1. Open the file `src/utils/firebase.js.template`.
2. Replace the configuration values with the ones from your Firebase project > Web App
3. Rename `firebase.js.template` to `firebase.js`.

## How to run
### Using npm
#### Start development server

```bash
> npm install
> npm run dev
```

#### or build and run

```bash
> npm install
> npm run build
> npm run start
```

Open [http://localhost:3000/](http://localhost:3000/)

### Using Docker

Build the image:

```bash
docker build -t web-rtc-share-web:1.0 . 
```

Run the container:

```bash
docker run --rm -d -p 8080:8080 localhost/web-rtc-share-web:1.0
```
