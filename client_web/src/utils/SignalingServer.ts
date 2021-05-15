import { firebase, realTimeDatabase } from './firebase';

const db = realTimeDatabase;

type SignalingMessage = {
  description?: RTCSessionDescription;
  candidate?: RTCIceCandidate;
};

class SignalingServer {
  roomId: string;

  isInitiator: boolean;

  myMessagesFolder: string;

  peerMessagesFolder: string;

  messagesFolderRef: firebase.database.Reference;

  constructor(roomId, isInitiator) {
    this.roomId = roomId;
    this.isInitiator = isInitiator;

    if (this.isInitiator) {
      this.myMessagesFolder = 'initiatorMessages';
      this.peerMessagesFolder = 'nonInitiatorMessages';
    } else {
      this.myMessagesFolder = 'nonInitiatorMessages';
      this.peerMessagesFolder = 'initiatorMessages';
    }

    // Description listener ref
    this.messagesFolderRef = null;
  }

  async sendMessage({ description, candidate } : SignalingMessage) {
    let message;
    if (description) {
      message = { description: JSON.stringify(description) };
    } else if (candidate) {
      message = { candidate: JSON.stringify(candidate) };
    }

    if (message) {
      const newMessageRef = db.ref(`rooms/${this.roomId}/${this.myMessagesFolder}`).push();
      await newMessageRef.set(message);
    }
  }

  setOnMessageListener(callback) {
    this.removeOnMessageListener();
    this.messagesFolderRef = db.ref(`rooms/${this.roomId}/${this.peerMessagesFolder}`);
    this.messagesFolderRef.on('child_added', async (data) => {
      if (!data) return;
      const { description, candidate } = data.val();
      if (description) {
        callback({ description: JSON.parse(description) });
      } else if (candidate) {
        callback({ candidate: JSON.parse(candidate) });
      }
    });
  }

  removeOnMessageListener() {
    if (this.messagesFolderRef) this.messagesFolderRef.off();
  }

  static HOST = 'HOST';

  static GUEST = 'GUEST';
}

export default SignalingServer;
