import java.net.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.Scanner;
import java.io.*;
import java.math.BigInteger;

public class peerProcess {

    // Peer variables
    int                 ID                  = 0;
    int                 bitFieldSize;
    int                 portNum;
    int[]               bitfield            = new int[16];
    Server              server              = null;
    Connection          optUnchoked;
    messageHandler      messenger           = new messageHandler(this, bitFieldSize);
    ConnectionHandler   connector           = new ConnectionHandler(this);
    Vector<Connection>  connections         = new Vector<>();
    Vector<Connection>  prefConnections;

    // Common variables
    int     numPrefferedConnections;
    int     unchokeInterval;
    int     optimisticUnchokeInterval;
    String  filename;
    long    fileSize;
    long    pieceSize;

    public peerProcess(int id)
    {
        this.ID = id;
    }

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
            connector.listen();
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
                    // Set the peer variables
                    String hostName = components[1];
                    int portNum = Integer.parseInt(components[2]);
                    boolean hasFile = (Integer.parseInt(components[3]) != 0);

                    // Establish a connection to peer
                    connector.requestConnection(pID, hostName, portNum, hasFile);
                }
            }

            peerReader.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        }
    }

    public Connection getPeer(int peerID) {
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).peerID == peerID) {
                return connections.get(i);
            }
        }

        return null;
    }

    public boolean receiveMessage(String msg, ObjectOutputStream out, int connectionID) {
        messenger.decodeMessage(msg, out, connectionID);
        return true;
    }

    public boolean sendMessage(int type, String msg, ObjectOutputStream out, int connectionID) {
        messenger.sendMessage(type, msg, out, connectionID);
        return true;
    }
}
