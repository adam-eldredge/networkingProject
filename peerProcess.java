import java.net.*;
import java.io.*;

public class peerProcess {
    
    class Connection {
        public int peerID;
        public int[] peerBitfield;
        
        // State variables
        public boolean choked = false; // Are they set to choked from us to them (not them to us)
        public boolean interested = false;
    }

    public peerProcess(int id) {
        this.ID = id;
        this.client = new Client(this);
        this.server = new Server(this);
    }

    // Peer variables
    int             ID              = 0;
    int             bitFieldSize;
    int[]           bitfield;
    Client          client          = null;
    Server          server          = null;
    Connection[]    connections;
    Connection[]    prefferedConnections;
    Connection      optimisticallyUnchoked;

    public void run() {
        System.out.println("Peer " + ID +  " started");
    }

    // decode message - returns message type with payload
    public void handleMessage(String msg) {
        try {
            
            int length = Integer.parseInt(msg.substring(0,4));
            int type = Integer.parseInt(msg.substring(5,6));

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
                handleHave(length);
            }
            else if (type == 5) {
                handleBitfield(msg.substring(6, 6 + length));
            }
            else if (type == 6) {
                handleRequest(length);
            }
            else if (type == 7) {
                handlePiece(length);
            }
            else {
                // Something is wrong
                return;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // *** MESSAGE HANDLING *** //
    public void handleChoke() { /* TODO */ }

    public void handleUnchoke() { /* TODO */ }

    public void handleInterested() { /* TODO */ }

    public void handleUninterested() { /* TODO */ }

    public void handleHave(int len) { /* TODO */}

    public void handleBitfield(String peerBitfield) {
        if (peerBitfield.length() == 0) {
            // Set connections bitfield to empty all zero
            return;
        }
        else {
            int[] receivedBitfield = new int[bitFieldSize];

            for (int i = 0; i < peerBitfield.length(); i++) {
                receivedBitfield[i] = peerBitfield.charAt(i);
            }

            // We need to compare the two bitfields and keep track of which bits we are interested in
            boolean interested = false;
            for (int i = 0; i < bitFieldSize; i++) {
                if ((receivedBitfield[i] + bitfield[i]) % 2 == 1) {
                    interested = true;
                }
            }

            if (interested) {
                /* SEND INTERESTED MESSAGE BACK */
            }
            else {
                /* SEND UNINTERESTED MESSAGE BACK */
            }
        }
        return;
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
    public boolean verifyHandshakeResponse(String msg, int expectedID) { /* TODO */ return true;}

    //send a message to the output stream
    public void sendMessage(String msg) {
        try {
            //stream write the message
            client.out.writeObject(msg);
            client.out.flush();
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // Main
    public static void main (String[] args) {
        // Create a new peer
        peerProcess peer = new peerProcess(Integer.valueOf(args[0]));
        peer.run();
    }
}
