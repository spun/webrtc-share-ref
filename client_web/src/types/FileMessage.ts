type TextInfo = {
  timestamp: number,
  value: string,
};

type FileInfo = {
  filename: string,
  size: number, // In bytes
  hash: number, // Hexadecimal
  transferChannelId: number
};

export enum MessageType {
  TEXT,
  FILE,
}

export type ChannelMessage = {
  type: MessageType
};

export interface ChannelMessageText extends ChannelMessage {
  content: TextInfo
}

export interface ChannelMessageFile extends ChannelMessage {
  content: FileInfo
}

export type FileTransferReadyMessage = {
  isReady: boolean,
  maxMessageSize: number
};
