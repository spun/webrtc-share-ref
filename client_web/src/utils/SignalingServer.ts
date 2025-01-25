import { realTimeDatabase } from './firebase';
import { onChildAdded, ref, off, set, DatabaseReference, push, child } from "firebase/database";

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

  messagesFolderRef: DatabaseReference | null;

  constructor(roomId: string, isInitiator: boolean) {
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

  async sendMessage({ description, candidate }: SignalingMessage) {
    let message;
    if (description) {
      message = { description: JSON.stringify(description) };
    } else if (candidate) {
      message = { candidate: JSON.stringify(candidate) };
    }

    if (message) {
      const newMessage = push(child(ref(db), `rooms/${this.roomId}/${this.myMessagesFolder}`));
      await set(newMessage.ref, message);
    }
  }

  setOnMessageListener(callback: (any: any) => void) {
    this.removeOnMessageListener();
    this.messagesFolderRef = ref(db, `rooms/${this.roomId}/${this.peerMessagesFolder}`);
    onChildAdded(this.messagesFolderRef, (data) => {
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
    if (this.messagesFolderRef) off(this.messagesFolderRef)
  }

  static HOST = 'HOST';

  static GUEST = 'GUEST';
}

export default SignalingServer;
