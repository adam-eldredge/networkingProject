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
    int                 ID;
    int                 bitFieldSize        = 16;
    int                 portNum;
    int[]               bitfield            = new int[16];
    Server              server              = null;
    Neighbor          optUnchoked;
    messageHandler      messenger           = new messageHandler(this, bitFieldSize);
    Vector<Neighbor>  neighbors         = new Vector<>();
    Vector<Neighbor>  prefNeighbor     = new Vector<>();
    private Timer timer = null;

    // add all data exchanged to this hashmap: key = peerID, value = data amount
    HashMap<Integer, Integer> connectionsPrevIntervalDataAmount = new HashMap<>();
    PeerLogger          logger;
    Boolean             fileCompleted             = false;
    Boolean             terminate   = false;

    // list of indices of requested pieces
    int[]   requested;

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
        peer.start();


    }

    public void setup() {
        Scanner commonReader = null;
        Scanner peerReader = null;



        try {
            File common = new File("Common.cfg");
            commonReader = new Scanner(common);

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
        }
        catch (FileNotFoundException e) { 
            System.out.println("Could not find file");
        }finally{
            if (commonReader != null) {
                commonReader.close();
            }
        }

        // Now read the PeerInfo file and attempt to make connections to each of the prior peers
        try {
            File peers = new File("PeerInfo.cfg");
            peerReader = new Scanner(peers);

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
                    try {
                        server.start();
                    }
                    catch (Exception e) {
                        System.out.println("Something went wrong in the run method");
                    }
                    break;
                }
                else {
                    // Set the peer variables
                    String hostName = components[1];
                    int portNum = Integer.parseInt(components[2]);
                    boolean hasFile = (Integer.parseInt(components[3]) != 0);

                    // Connect our peer to the other peer
                    Neighbor priorPeer = new Neighbor(this, pID, hostName, portNum, hasFile);
                    
                    priorPeer.startClient();
                    messenger.sendMessage(MessageType.BITFIELD, getBitfieldString(), priorPeer.getOutputStream(),priorPeer.getInputStream(), priorPeer.neighborID);
                    // Add connection to this peers list of connections
                    this.neighbors.add(priorPeer);
                }
            }

        }
        catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        }finally{
            if (peerReader != null) {
                peerReader.close();
            }
        }

    }
    
    public void start() {
        
        try {
            timer = new Timer();
            long unchokedIntervalSeconds = unchokeInterval * 1000;
            long optimisticUnchokedIntervalSeconds = optimisticUnchokeInterval * 1000;

            // Schedule the updatePrefConnectionsTask to run every 'unchokeInterval' milliseconds
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Update the preferred connections
                    updatePrefConnections();

                    // can potentially terminate here by checking if all peers have the file and then calling closeNeighborConnections() and stopTimer()

                }
            }, 0, unchokedIntervalSeconds);

            //Schedule the updateOptUnchokedTask to run every 'optimisticUnchokeInterval' milliseconds
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Update the optimistic unchoked connections
                    updateOptUnchoked();

                    // can potentially terminate here by checking if all peers have the file and then calling closeNeighborConnections() and stopTimer()
                }
            }, 0, optimisticUnchokedIntervalSeconds);
        }
        catch (Exception e) {
            System.out.println("Something went wrong in the run method");
        }
        
    }
    public void stopTimer(){
        if (timer != null) {
            timer.cancel();
        }
    }

    private void updatePrefConnections() {
        // first = peerID, second = download rate
        PriorityQueue<Pair> maxPairQueue = new PriorityQueue<>(pairComparator);
        
        // key = peerID, value = connection
        HashMap<Integer, Neighbor> connectionsDownloadRate = new HashMap<>();
        
        //logger input parameter
        List<String> listOfPrefNeighbors = new ArrayList<String>();
        

        // all previous prev neighbors are set to choked unless they are optimisticallyunchoke neighbor
        for(int i = 0; i < prefNeighbor.size(); i++){
            Neighbor current = prefNeighbor.get(i);
            if(current != optUnchoked){
                messenger.sendMessage(MessageType.CHOKE, null, current.getOutputStream(), current.getInputStream(), current.neighborID);
            }
        }
        
        // clear the previous preferred connections
        prefNeighbor.clear();
        
        int count = numPrefferedConnections;

        //calculate preferred neighbors
        if(fileCompleted){
            // get k random neighbors
            for (int i = 0; i < neighbors.size(); i++) {
                Neighbor current = neighbors.get(i);
                if (current.themInterested && count != 0) {
                    prefNeighbor.add(current);
                    listOfPrefNeighbors.add(Integer.toString(current.neighborID));
                    count--;
                }
            }
        }
        else{
            // calculate download rate for each interested neighbor
            // rate = data amount/time(unchokedinterval)
            for (int i = 0; i < neighbors.size(); i++) {
                Neighbor current = neighbors.get(i);
                int dataAmount = 0;
                if (current.themInterested) {

                    if(connectionsPrevIntervalDataAmount.containsKey(current.neighborID)){
                        dataAmount = connectionsPrevIntervalDataAmount.get(current.neighborID);
                    }
                    maxPairQueue.add(new Pair(current.neighborID, dataAmount/unchokeInterval));
                    connectionsDownloadRate.put(current.neighborID, current);
                }
            }
            // get preffered connections for top k neighbors with highest download rate
            for(int i = 0; i < numPrefferedConnections; i++){
                if(maxPairQueue.isEmpty()){
                    break;
                }
                Pair pair = maxPairQueue.poll(); // Get and remove the maximum pair
                Neighbor current = connectionsDownloadRate.get(pair.key);
                prefNeighbor.add(current);
                listOfPrefNeighbors.add(Integer.toString(current.neighborID));
            }
        }
        
        
        connectionsPrevIntervalDataAmount.clear();
        
        logger.changePreferredNeighbors(listOfPrefNeighbors);
        
        for(int i = 0; i < prefNeighbor.size(); i++){
            Neighbor current = prefNeighbor.get(i);
            messenger.sendMessage(MessageType.UNCHOKE, null, current.getOutputStream(), current.getInputStream(), current.neighborID);
        }
        

    }

    private void updateOptUnchoked() {
        // Sort the connections by download rate
        Vector<Neighbor> candidatePool = new Vector<>();
        Random rand = new Random();
        for (int i = 0; i < neighbors.size(); i++) {
            Neighbor current = neighbors.get(i);
            if (current.themInterested && current.themChoked) {
                candidatePool.add(current);
            }
        }

        //if we dont need to change opt unchoked neighbor (this happens when neighbors.size() == 0 or !current.themInterested || !current.themChoked
        if (!candidatePool.isEmpty()) {
            optUnchoked = candidatePool.get(rand.nextInt(candidatePool.size()));
            logger.changeOptimisticallyUnchokedNeighbors(Integer.toString(optUnchoked.neighborID));
    
            // Send "UNCHOKE" message
            messenger.sendMessage(MessageType.UNCHOKE, null, optUnchoked.getOutputStream(), optUnchoked.getInputStream(), optUnchoked.neighborID);
        } else {
            // Handle the case where there are no valid candidates to choose from.
            return;
        }
    }

    private void closeNeighborConnections() {
        // Close all connections
        for (int i = 0; i < neighbors.size(); i++) {
            neighbors.get(i).closeClient();
        }
    }

    public boolean receiveMessage(String msg, ObjectOutputStream out, ObjectInputStream in,int connectionID) {
        messenger.decodeMessage(msg, out, in, connectionID);
        return true;
    }
    
    public boolean sendMessage(MessageType type, String msg, ObjectOutputStream out, ObjectInputStream in, int connectionID) {
        messenger.sendMessage(type, msg, out, in, connectionID);
        return true;
    }

    // Getters

    public Neighbor getPeer(int peerID) {
        for (int i = 0; i < neighbors.size(); i++) {
            if (neighbors.get(i).neighborID == peerID) {
                return neighbors.get(i);
            }
        }

        return null;
    }

    public PeerLogger getLogger() {
        return this.logger;
    }

    public String getBitfieldString() {
        String bitfieldString = "";
        for (int i = 0; i < bitfield.length; i++) {
            bitfieldString += Integer.toString(bitfield[i]);
        }
        return bitfieldString;
    }

}
