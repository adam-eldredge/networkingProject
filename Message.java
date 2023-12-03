public class Message {
    // Message Length(4 bytes)=payload.length+1 / messgae Type(1 byte) / message payload()
    float messageLength;
    byte messageType;
    byte[] messagePayload;

    Message(byte messageType, byte[] messagePayload) {
        this.messageType = messageType;
        this.messagePayload = messagePayload;
        this.messageLength = messagePayload.length + 1;
    }
    Message(byte messageType) {
        this.messageType = messageType;
        this.messagePayload = null;
        this.messageLength = 1;
    }
}
