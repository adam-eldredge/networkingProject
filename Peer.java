import java.net.*;
import java.io.*;
import javafx.util;

public class Peer {
    // Class to maintain state information about connections
    class State {
        public boolean choked = false;
        public boolean interested = false;
    }

    // Class to maintain connections
    class Connection {
        public int peerID;
        public int[] peerBitfield;
        public State state;
    }

    // Parent class for client and server
    int ID = 0;
    int[] bitfield;
    int bitFieldSize;
    Connection[] connections;

    // decode message - returns message type with payload
    public void decodeMessage(String msg) {
        try {
            
            int length = Integer.parseInt(msg.Substring(0,4));
            int type = Integer.parseInt(msg.Substring(5,6));

            if (type == 0) {
                handleChoke();
            }
            else if (type == 1) {
                handleUnchoke();
            }
            else if (type == 2) {
                handleInterested();
            }
            else if (type == 3) {
                handleUninterested();
            }
            else if (type == 4) {
                handleHave(int len);
            }
            else if (type == 5) {
                handleBitfield(int len);
            }
            else if (type == 6) {
                handleRequest(int len);
            }
            else if (type == 7) {
                handlePiece(int len);
            }
            else {
                // Something is wrong
                break;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleChoke() { /* TODO */ }

    public void handleUnchoke() { /* TODO */ }

    public void handleInterested() { /* TODO */ }

    public void handleUninterested() { /* TODO */ }

    public void handleHave(int len) { /* TODO */}

    public void handleBitfield(int len) { /* TODO */ }

    public void handleRequest(int len) { /* TODO */ }

    public void handlePiece(int len) { /* TODO */ }

    byte[] intToByteArray(int value) {
        byte[] intBytes = new byte[4];
        intBytes[0] = (byte) (value >> 24 & 0xFF);
        intBytes[1] = (byte) (value >> 16 & 0xFF);
        intBytes[2] = (byte) (value >> 8 & 0xFF);
        intBytes[3] = (byte) (value & 0xFF);
        return intBytes;
    }

    5() {
        // look at your bitfield and theirs
        // If they have something you want, send the interested message
        // If not, send uninterested

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
