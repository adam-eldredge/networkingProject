import java.net.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.Scanner;
import java.io.*;
import java.math.BigInteger;

public class PeerProcess {
    
    class Connection {
        public int peerID;
        public String hostName;
        public int portNum;
        public boolean hasFile;

        // Bitfield information
        public int[] peerBitfield;
        
        // State variables
        public boolean choked = false; // Are they set to choked from us to them (not them to us)
        public boolean interested = false;

        public Client peerClient = null;
    }

    public PeerProcess(int id)
    {
        this.ID = id;
    }

    // Peer variables
    int                     ID              = 0;
    int                     bitFieldSize;
    int[]                   bitfield = new int[16];
    Server                  server          = null;
    Vector<Connection>   connections     = new Vector<>();
    Vector<Connection>   prefferedConnections;
    Connection              optimisticallyUnchoked;
    int portNum;

    // Common variables
    int numPrefferedConnections;
    int unchokeInterval;
    int optimisticUnchokeInterval;
    String filename;
    long fileSize;
    long pieceSize;

    public void run() {
        System.out.println("Peer " + ID +  " started");
        
        try {
            /* START SERVER */
            server.run();
        }
        catch (Exception e) {
            System.out.println("Something went wrong");
        }
        
    }

    // decode message - returns message type with payload
    public void handleMessage(String msg) {
        try {
            int length = Integer.parseInt(msg.substring(0,4));
            int type = Integer.parseInt(msg.substring(4,5));

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
    public void handleChoke() { System.out.println("Handled the choke"); }

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
                sendInterested();
            }
            else {
                sendUnchoke();
            }
        }
        return;
    }

    public void handleRequest(int len) { /* TODO */ }

    public void handlePiece(int len) { /* TODO */ }

    // *** MESSAGE SENDING *** //
    public void sendChoke() {
        String msg = "00010";
        //sendMessage(msg);
    }
    public void sendUnchoke() {
        String msg = "00011";
        //sendMessage(msg);
    }
    public void sendInterested() {
        String msg = "00012";
        //sendMessage(msg);
    }
    public void sendUninterested() {
        String msg = "00013";
        //sendMessage(msg);
    }

    public void sendHave(int len) { /* TODO */}

    public void sendBitfield() {
        //sendMessage(Arrays.toString(peer.bitfield));
    }

    public void sendRequest(int len) { /* TODO */ }

    public void sendPiece(int len) { /* TODO */ }

    public void setup() {
        try {
            File common = new File("Common.cfg");
            Scanner commonReader = new Scanner(common);

            // Num preffered neighbors
            commonReader.next();
            this.numPrefferedConnections = Integer.parseInt(commonReader.next());

            // Unchoking interval
            commonReader.next();
            this.unchokeInterval = Integer.parseInt(commonReader.next());

            // Optimistic unchoking interval
            commonReader.next();
            this.optimisticUnchokeInterval = Integer.parseInt(commonReader.next());

            // filename
            commonReader.next();
            this.filename = commonReader.next();

            // Filesize
            commonReader.next();
            this.fileSize = Long.parseLong(commonReader.next());

            // Piecesize
            commonReader.next();
            this.pieceSize = Long.parseLong(commonReader.next());

            commonReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        }

        // Now read the PeerInfo file and attempt to make connections to each of the prior peers
        try {
            File peers = new File("PeerInfo.cfg");
            Scanner peerReader = new Scanner(peers);

            while (peerReader.hasNextLine()) {
                String peerLine = peerReader.nextLine();
                String[] components = peerLine.split(" ");

                // Check the host ID
                int pID = Integer.parseInt(components[0]);
                if (this.ID == pID) {
                    this.portNum = Integer.parseInt(components[2]);
                    this.server = new Server(this, portNum);
                    break;
                }
                else {
                    // Connect our peer to the other peer
                    Connection priorPeer = new Connection();

                    // Set the peer variables
                    priorPeer.peerID = pID;
                    priorPeer.hostName = components[1];
                    priorPeer.portNum = Integer.parseInt(components[2]);
                    priorPeer.hasFile = (Integer.parseInt(components[3]) != 0);

                    // Add connection to this peers list of connections
                    this.connections.add(priorPeer);
                }
            }

            peerReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        }
    
        // Now make connections to all prior peers found
        for (int i = 0; i < this.connections.size(); i++) {
            System.out.println("Creating client to connect to peer: " + connections.get(i).peerID);
            Connection current = connections.get(i);
            current.peerClient = new Client(this, current.hostName, current.portNum, current.peerID);
            current.peerClient.run();
        }
    }

    // Main
    public static void main (String[] args) {
        // Create a new peer
        PeerProcess peer = new PeerProcess(Integer.valueOf(args[0]));
        // Read the config files
        peer.setup();

        // Start the peer
        peer.run();
    }
}
