import java.net.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.Scanner;
import java.io.*;
import java.math.BigInteger;

public class peerProcess {
    
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

    public peerProcess(int id)
    {
        this.ID = id;
    }

    // Peer variables
    int                 ID                  = 0;
    int                 bitFieldSize;
    int                 portNum;
    int[]               bitfield            = new int[16];
    Server              server              = null;
    Connection          optUnchoked;
    messageHandler      messenger           = new messageHandler(this, bitFieldSize);
    Vector<Connection>  connections         = new Vector<>();
    Vector<Connection>  prefConnections;

    // Common variables
    int     numPrefferedConnections;
    int     unchokeInterval;
    int     optimisticUnchokeInterval;
    String  filename;
    long    fileSize;
    long    pieceSize;

    // Main
    public static void main (String[] args) {
        // Create a new peer
        peerProcess peer = new peerProcess(Integer.valueOf(args[0]));
        // Read the config files
        peer.setup();

        // Start the peer
        peer.run();
    }

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

    public boolean receiveMessage(String msg, ObjectOutputStream out) {
     
        messenger.decodeMessage(msg, out);
        return true;
    }

    public boolean sendMessage(int type, String msg, ObjectOutputStream out) {
        messenger.sendMessage(type, msg, out);
        return true;
    }
}
