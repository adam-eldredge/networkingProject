import java.net.*;
import java.io.*;

public class Peer {
    // Parent class for client and server
    int peerID = 0;

    // decode message - returns message type with payload
    public string decodeMessage(String msg) {
        try {
            // First 4 bytes are message length
            int length = Integer.parseInt(msg.Substring(0,4));

            // Next byte is message type
            int type = Integer.parseInt(msg.Substring(5,6));

            if (type == 0) {
                // Choke, Unchoke, Interested, or not interested
                // No payload
            }
            else if (type == 1) {
                // Have
            }
            else if (type == 2) {
                // Have
            }
            else if (type == 3) {
                // Have
            }
            else if (type == 4) {
                // Have
            }
            else if (type == 5) {
                // Bitfield - 4 byte piece index field
            }
            else if (type == 6) {
                // Request - 4 byte piece index field
            }
            else if (type == 7) {
                // Piece - 4 byte piece index field and the content of the piece
            }
            else {
                // Something is wrong
            }

            // End is payload
            String payload = msg.Substring(6,(6 + length));

            return payload;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    byte[] intToByteArray(int value) {
        byte[] intBytes = new byte[4];
        intBytes[0] = (byte) (value >> 24 & 0xFF);
        intBytes[1] = (byte) (value >> 16 & 0xFF);
        intBytes[2] = (byte) (value >> 8 & 0xFF);
        intBytes[3] = (byte) (value & 0xFF);
        return intBytes;
    }

    public void sendHandshakeMessage() {
        String header = "P2PFILESHARINGPROJ";
        String zeros = "0000000000";
        //Insert Correct PeerID from config file
        int peerID = 69;

        //Convert everything to bytes
        byte[] headerBytes = header.getBytes();
        byte[] zeroBytes = zeros.getBytes();
        byte[] idBytes = intToByteArray(peerID);

        //Initialize 32 byte container
        byte[] msg = new byte[32];

        //Copy bytes into the array
        System.arraycopy(headerBytes, 0, msg, 0, 18);
        System.arraycopy(zeroBytes, 0, msg, 18, 10);
        System.arraycopy(idBytes, 0, msg, 28, 4);

        //Do we want to send as byte[] or as String??
        String msgString = new String(msg);
        sendMessage(msgString);
    }

    //send a message to the output stream
    void sendMessage(String msg) {
        try {
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
