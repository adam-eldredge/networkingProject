import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.Scanner;
import java.io.*;
import java.math.BigInteger;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;

public class peerProcess {
    
    // Peer variables
    int                 ID                  = 0;
    int                 bitFieldSize;
    int                 portNum;
    int[]               bitfield            = new int[16];
    Server              server              = null;
    Connection          optUnchoked;
    messageHandler      messenger           = new messageHandler(this, bitFieldSize);
    Vector<Connection>  connections         = new Vector<>();
    Vector<Connection>  prefConnections     = new Vector<>();

    // add all data exchanged to this hashmap: key = peerID, value = data amount
    HashMap<Integer, Integer> connectionsPrevIntervalDataAmount = new HashMap<>();
    PeerLogger          logger;
    Boolean             fileCompleted             = false;


    // Common variables
    int     numPrefferedConnections;
    int     unchokeInterval;
    int     optimisticUnchokeInterval;
    String  filename;
    long    fileSize;
    long    pieceSize;
    
    private class Pair {
        int key;
        int value;
    
        public Pair(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    Comparator<Pair> pairComparator = new Comparator<Pair>() {
        @Override
        public int compare(Pair pair1, Pair pair2) {
            return Integer.compare(pair2.value, pair1.value);
        }
    };


    public peerProcess(int id)
    {
        this.ID = id;
        this.logger = new PeerLogger(id);
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

                    // Set the bitfield
                    Arrays.fill(this.bitfield, Integer.parseInt(components[3]));
                    fileCompleted = (Integer.parseInt(components[3]) == 1);
                    
                    // start the server
                    this.server = new Server(this, portNum);
                    break;
                }
                else {
                    // Set the peer variables
                    String hostName = components[1];
                    int portNum = Integer.parseInt(components[2]);
                    boolean hasFile = (Integer.parseInt(components[3]) != 0);

                    // Connect our peer to the other peer
                    Connection priorPeer = new Connection(pID, hostName, portNum, hasFile);

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
    
    public void run() {
        Timer timer = new Timer();
        // System.out.println("Peer " + ID +  " started");
        TimerTask updatePreConnectionsTask = new TimerTask() {
            @Override
            public void run() {
                // Update the preferred connections
                updatePrefConnections();
            }
        };
        TimerTask updateOptUnchokedTask = new TimerTask() {
            @Override
            public void run() {
                // Update the preferred connections
                updateOptUnchoked();
            }
        };
        
        try {
            // schedule the tasks
            timer.schedule(updatePreConnectionsTask, unchokeInterval);
            timer.schedule(updateOptUnchokedTask, optimisticUnchokeInterval);
        
            /* START SERVER */
            server.run();

            //cancel the timer
            // timer.cancel();
        }
        catch (Exception e) {
            System.out.println("Something went wrong in the run method");
        }
        
    }

    private void updatePrefConnections() {
        // first = peerID, second = download rate
        PriorityQueue<Pair> maxPairQueue = new PriorityQueue<>(pairComparator);
        
        // key = peerID, value = connection
        HashMap<Integer, Connection> connectionsDownloadRate = new HashMap<>();
        
        //logger input parameter
        List<String> listOfPrefNeighbors = new ArrayList<String>();
        
        // clear the previous preferred connections
        prefConnections.clear();

        int count = numPrefferedConnections;

        if(fileCompleted){
            // get k random neighbors
            for (int i = 0; i < connections.size(); i++) {
                Connection current = connections.get(i);
                if (current.themInterested && count != 0) {
                    prefConnections.add(current);
                    listOfPrefNeighbors.add(Integer.toString(current.peerID));
                    count--;
                }
            }
        }
        else{
            // calculate download rate for each interested neighbor
            // rate = data amount/time(unchokedinterval)
            for (int i = 0; i < connections.size(); i++) {
                Connection current = connections.get(i);
                int dataAmount = 0;
                if (current.themInterested) {

                    if(connectionsPrevIntervalDataAmount.containsKey(current.peerID)){
                        dataAmount = connectionsPrevIntervalDataAmount.get(current.peerID);
                    }
                    maxPairQueue.add(new Pair(current.peerID, dataAmount/unchokeInterval));
                    connectionsDownloadRate.put(current.peerID, current);
                }
            }
            // get preffered connections for top k neighbors with highest download rate
            for(int i = 0; i < numPrefferedConnections; i++){
                Pair pair = maxPairQueue.poll(); // Get and remove the maximum pair
                Connection current = connectionsDownloadRate.get(pair.key);
                prefConnections.add(current);
                listOfPrefNeighbors.add(Integer.toString(current.peerID));
            }
        }
        
        connectionsPrevIntervalDataAmount.clear();
        logger.changePreferredNeighbors(listOfPrefNeighbors);

        //unchoked pref neighbors and expects to recieve request message(if neighbor us choked)

        // all previous prev neighbors are set to choked unless they are optimisticallyunchoke neighbor

    }

    private void updateOptUnchoked() {
        // Sort the connections by download rate
        Vector<Connection> candidatePool = new Vector<>();
        Random rand = new Random();
        for (int i = 0; i < connections.size(); i++) {
            Connection current = connections.get(i);
            if (current.themInterested && current.themChoked) {
                candidatePool.add(current);
            }
        }
        optUnchoked = candidatePool.get(rand.nextInt(candidatePool.size()));
        logger.changeOptimisticallyUnchokedNeighbors(Integer.toString(optUnchoked.peerID));

        // send unchocked and expects request message
        // if prev optunchoked if not in prefconnections send chock
    }

    public Connection getPeer(int peerID) {
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).peerID == peerID) {
                return connections.get(i);
            }
        }

        return null;
    }

    public PeerLogger getLogger() {
        return this.logger;
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
