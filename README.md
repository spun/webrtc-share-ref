# About

*This project is part of a series of small and quick projects created to have some code references for future creations. Be aware, these projects are not focused on best practices, testing or code quality in general, their purpose is to be the first steps to a more solid implementation.*

This project contains a Web page and an Android application that make use of a WebRTC connection to share data. WebRTC is known for its videoconferencing capabilities, but in this project we are interested only on WebRTC DataChannels that can be used to send and receive text and binaries. This apps are not for video or audio.

WebRTC requires a starting negotation process commonly called "signaling" to discover each other before making a connection. This signaling process can be done with any kind of server able to deliver those negotation messages to the other peer. In this project we are using Firebase RealtimeDatabase as our signaling server because, for our purpose, it's free and easy to deploy.

If you want to develop your own signaling server, a good easy-to-develop alternative is the use of WebSockets.

## What does this project use

What can this project offer as a reference for the future?

- [x] WebRTC
  - [x] Send text messages
  - [ ] Send files
- [x] [Kotlin serialization](https://github.com/Kotlin/kotlinx.serialization)
- [x] Firebase RealtimeDatabase

## How to run

### Signaling server

- Create a new Firebase Project.
- Add a new Android app inside the firebase project, download the `google-services.json` configuration file an put it inside the `client_android/app` folder.
- Add a new Web app inside the firebase project, copy the `firebaseConfig` values an replace the ones inside `client_web/src/utils/firebase.js.template`. Finally, rename `firebase.js.template` to `firebase.js`.

### Web client

Navigate to the web client folder and run:

```bash
> npm install
> npm run dev
```

Open [http://localhost:3000/](http://localhost:3000/)

### Android client

Open the project in AndroidStudio and run the application.

## Sources

- [MDN WebRTC API documentation](https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API)
- [W3C WebRTC 1.0 document](https://www.w3.org/TR/webrtc/)

---

Logo from [Twemoji](https://twemoji.twitter.com/ "Twemoji") licensed under [CC-BY 4.0](https://creativecommons.org/licenses/by/4.0/ "CC-BY 4.0")
