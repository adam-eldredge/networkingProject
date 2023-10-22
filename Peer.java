import java.net.*;
import java.io.*;
import java.javafx.util;
import client.Client;
import server.Server;

public class Peer {
    // Class to maintain state information about connections
    class State {
        public boolean choked = false; // Are they set to choked from us to them (not them to us)
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
    Client client = new Client();
    Server server = new Server();
    int[] bitfield;
    int bitFieldSize;
    Connection[] connections;

    // decode message - returns message type with payload
    public void handleMessage(String msg) {
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
                handleHave(len);
            }
            else if (type == 5) {
                handleBitfield(msg.Substring(6, 6 + length));
            }
            else if (type == 6) {
                handleRequest(len);
            }
            else if (type == 7) {
                handlePiece(len);
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

    // *** MESSAGE HANDLING *** //
    public void handleChoke() { /* TODO */ }

    public void handleUnchoke() { /* TODO */ }

    public void handleInterested() { /* TODO */ }

    public void handleUninterested() { /* TODO */ }

    public void handleHave(int len) { /* TODO */}

    public boolean handleBitfield(string bitfield) {
        if (bitfield.length() == 0) {
            // Peer does not yet have anything
        }
        else {
            // We need to compare the two bitfields and keep track of which bits we are interested in
            boolean interested = false;
            for (int i = 0; i < /* Insert Bitfield Length */10; i++) {
                if ((bitfield[i] + connections[0].peerBitfield[i]) % 2 == 1) {
                    interested = true;
                }
            }

            if (interested) {
                // We need a function to send an interested message to the peer
            }
            else {
                // Function to send an uninterested message to the peer
            }
        }
    }

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

    // Handshake response verification
    public boolean verifyHandshakeResponse(string msg, int expectedID) { /* TODO */}

    //send a message to the output stream
    public void sendMessage(String msg) {
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
