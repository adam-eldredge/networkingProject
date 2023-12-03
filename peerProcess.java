import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;
import java.io.*;
import java.math.BigInteger;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

public class peerProcess {

    // Peer variables
    int ID;
    int portNum;
    volatile int currentNumPieces = 0;
    volatile Bitfield bitfield;
    //Piecefield piecefield;
    volatile Piece[] pieces;
    public byte[] piecesIdxMap; 

    volatile byte[] filebytes;
    Server server = null;
    Neighbor optUnchoked = null;
    messageHandler messenger = new messageHandler(this);

    volatile Vector<Neighbor> neighbors = new Vector<>();

    // key = peerID, value = neighbor
    public HashMap<Integer, Neighbor> neighborMap = new HashMap<>();
    
    Vector<Neighbor> prefNeighbor = new Vector<>();
    private Timer timer = null;

    // key = peerID, value = true if peer has file
    public HashMap<Integer, Boolean> completedPeerTracker = new HashMap<>();
    //key = requestedIdx, value = peerID
    public volatile HashMap<Integer, Integer> requestedIdxTracker = new HashMap<>();

    public void setCompletedPeer(int peerID) {
        completedPeerTracker.put(peerID, true);
    }

    // add all data exchanged to this hashmap: key = peerID, value = data amount
    volatile HashMap<Integer, Integer> connectionsPrevIntervalDataAmount = new HashMap<>();

    volatile PeerLogger logger;
    Boolean fileCompleted = false;
    Boolean terminate = false;

    // Common variables
    int numPrefferedConnections;
    int unchokeInterval;
    int optimisticUnchokeInterval;
    String filename;
    long fileSize;
    long pieceSize;

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

    public peerProcess(int id) throws IOException {
        this.ID = id;
        this.logger = new PeerLogger(id);
    }

    // Main
    public static void main(String[] args) {

        // Create a new peer
        peerProcess peer;
        try {
            peer = new peerProcess(Integer.valueOf(args[0]));

            // Read the config files
            peer.setup();

            // Start the peer
            peer.start();

        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void setupPieces(boolean hasFile) {
        int numPieces = (int) fileSize / (int) pieceSize;
        byte[] filebytes;

        if ((int) fileSize % (int) pieceSize != 0) {
            numPieces++;
        }
        this.pieces = new Piece[numPieces];

        if(hasFile){
            //read bytes from file
            try {
                filebytes = Files.readAllBytes(Paths.get(this.filename));
            } catch (IOException e) {
                // Handle file reading exception
                e.printStackTrace();
                return;
            }
        } else {
            //create empty byte array
            filebytes = new byte[(int) fileSize];
        }

        for (int i = 0; i < numPieces; i++) {
            int startIndex = i * (int)pieceSize;
            int endIndex = Math.min(startIndex + (int)pieceSize, filebytes.length);
            
            byte[] pieceData = Arrays.copyOfRange(filebytes, startIndex, endIndex);
            this.pieces[i] = new Piece(i, pieceData);
        }
    }
    public void setPiece(int idx, byte[] data){
        this.pieces[idx].setData(data);
        updatePiecesIdxMap(idx);
    }
    private void updatePiecesIdxMap(int index) {
        //increment the number of pieces
        this.currentNumPieces++;
        int counter = 0;
        for (byte b : piecesIdxMap) {
            for (int i = 0; i < 8; i++) { // Assuming 8 bits per byte
                if (counter == index) {
                    //change the bit to 1
                    b |= (1 << (7 - i));
                    return;
                }
                counter++;
            }
        }
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

            int size = (int) fileSize / (int) pieceSize;

            if ((int) fileSize % (int) pieceSize != 0) {
                size++;
            }
            this.messenger.setNumPieces((int) pieceSize);

        } catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        } finally {
            if (commonReader != null) {
                commonReader.close();
            }
        }

        // Now read the PeerInfo file and attempt to make connections to each of the
        // prior peers
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

                    // Set the bitfield and read
                    if (Integer.parseInt(components[3]) == 1) {
                        setupPieces(true);
                        currentNumPieces = pieces.length;
                        fileCompleted = true;
                    } else {
                        currentNumPieces = 0;
                        setupPieces(false);
                    }

                    // start the server
                    this.server = new Server(this, portNum);
                    try {
                        server.start();
                    } catch (Exception e) {
                        System.out.println("Something went wrong in the run method");
                    }
                    break;
                } else {
                    // Set the peer variables
                    String hostName = components[1];
                    int portNum = Integer.parseInt(components[2]);
                    boolean hasFile = (Integer.parseInt(components[3]) != 0);

                    // Create a new Client for this connection
                    new Client(this, hostName, portNum, pID, hasFile);

                }
            }

            // Start all the client connections
            for(Neighbor n : neighborMap.values()){
                if(n.type == ConnectionType.CLIENT){
                    n.getConnection().start();
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        } finally {
            if (peerReader != null) {
                peerReader.close();
            }
        }

        try {
            File peers = new File("PeerInfo.cfg");
            peerReader = new Scanner(peers);

            while (peerReader.hasNextLine()) {
                String peerLine = peerReader.nextLine();
                String[] components = peerLine.split(" ");
                // Check the host ID
                int pID = Integer.parseInt(components[0]);

                completedPeerTracker.put(pID, components[3].equals("1"));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not find file");
        } finally {
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

            // Schedule the updatePrefConnectionsTask to run every 'unchokeInterval'
            // milliseconds
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Update the preferred connections
                    checkTermination();
                    updatePrefConnections();

                }
            }, 0, unchokedIntervalSeconds);

            // Schedule the updateOptUnchokedTask to run every 'optimisticUnchokeInterval'
            // milliseconds
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // Update the optimistic unchoked connections
                    checkTermination();
                    updateOptUnchoked();

                    // can potentially terminate here by checking if all peers have the file and
                    // then calling closeNeighborConnections() and stopTimer()
                }
            }, 0, optimisticUnchokedIntervalSeconds);

        } catch (Exception e) {
            System.out.println("Something went wrong in the run method");
        }

    }

    public void terminate() {
        // Stop the timer
        try {
            if (timer != null) {
                timer.cancel();
            }

            // Close all connections
            closeNeighborConnections();
            System.out.println("Closed all connections");

            // Might not be needed Close the server
            server.terminate();
            System.out.println("Closed the server");

            // Close the logger
            logger.closeLogger();
            System.out.println("Closed the logger");
        } catch (Exception e) {
            System.out.println("Something went wrong in the terminate method");
        }
    }

    private void closeNeighborConnections() {
        // Close all connections
        for (int i = 0; i < neighbors.size(); i++) {
            Neighbor currentNeighbor = neighbors.get(i);
            currentNeighbor.getConnection().terminate();
        }
    }

    private void updatePrefConnections() {
        try {

            // first = peerID, second = download rate
            // data structure to sort neighbors by download rate
            PriorityQueue<Pair> maxPairQueue = new PriorityQueue<>(pairComparator);

            // key = peerID, value = neighbor
            HashMap<Integer, Neighbor> connectionsDownloadRate = new HashMap<>();

            // List of prefer neibors IDs to send to the message logger
            // List<String> listOfPrefNeighbors = new ArrayList<String>();

            //
            List<String> newPrefNeighbors = new ArrayList<String>();
            // Vector<Neighbor> prefNeighbor = new Vector<>();

            int count = numPrefferedConnections;

            // calculate preferred neighbors
            if (fileCompleted) {
                // get k random neighbors
                for (int i = 0; i < neighbors.size(); i++) {
                    Neighbor current = neighbors.get(i);
                    if (current.getInterested() && count != 0) {
                        newPrefNeighbors.add(Integer.toString(current.neighborID));
                        // listOfPrefNeighbors.add(Integer.toString(current.neighborID));
                        count--;
                    }
                }
            } else {
                // calculate download rate for each interested neighbor
                // rate = data amount/time(unchokedinterval)
                for (int i = 0; i < neighbors.size(); i++) {
                    Neighbor current = neighbors.get(i);
                    int dataAmount = 0;
                    if (current.getInterested()) {

                        if (connectionsPrevIntervalDataAmount.containsKey(current.neighborID)) {
                            dataAmount = connectionsPrevIntervalDataAmount.get(current.neighborID);
                        }
                        maxPairQueue.add(new Pair(current.neighborID, dataAmount / unchokeInterval));
                        connectionsDownloadRate.put(current.neighborID, current);
                    }
                }
                // get preffered connections for top k neighbors with highest download rate
                for (int i = 0; i < numPrefferedConnections; i++) {
                    if (maxPairQueue.isEmpty()) {
                        break;
                    }
                    Pair pair = maxPairQueue.poll(); // Get and remove the maximum pair
                    Neighbor current = connectionsDownloadRate.get(pair.key);
                    newPrefNeighbors.add(Integer.toString(current.neighborID));
                    // listOfPrefNeighbors.add(Integer.toString(current.neighborID));
                }
            }

            // loop through all pref neighbors and send choke to any not in new list
            for (int i = 0; i < prefNeighbor.size(); i++) {
                Neighbor current = prefNeighbor.get(i);

                if (!newPrefNeighbors.contains(Integer.toString(current.neighborID))
                        && (optUnchoked != null && current.neighborID != optUnchoked.neighborID)) {
                    current.setChoked(true);
                    System.out.println("Sending choke to " + current.neighborID);
                    sendMessage(MessageType.CHOKE, current.getOutputStream(), current.getInputStream(),
                            current.neighborID, -1);
                }
            }
            // update pref neibors with newPrefNeibor O-n^2
            prefNeighbor.clear();

            for (int i = 0; i < newPrefNeighbors.size(); i++) {
                Neighbor current = getPeer(Integer.parseInt(newPrefNeighbors.get(i)));
                prefNeighbor.add(current);
            }

            connectionsPrevIntervalDataAmount.clear();
            logger.changePreferredNeighbors(newPrefNeighbors);

            // loop pref neighbor and send unchoke to any not choked
            for (int i = 0; i < prefNeighbor.size(); i++) {
                Neighbor current = prefNeighbor.get(i);
                // TimeUnit.SECONDS.sleep(1);
                if (current != optUnchoked && current.getChoked()) {
                    current.setChoked(false);
                    System.out.println("Sending unchoke to " + current.neighborID);
                    sendMessage(MessageType.UNCHOKE, current.getOutputStream(), current.getInputStream(),
                            current.neighborID, -1);
                    System.out.println("Sent unchoke to " + current.neighborID);
                }
            }

        } catch (Exception e) {
            System.out.println("Something went wrong in the updatePrefConnections method");
        }

    }

    private void updateOptUnchoked() {
        // Sort the connections by download rate
        try {
            Vector<Neighbor> candidatePool = new Vector<>();
            Random rand = new Random();
            for (int i = 0; i < neighbors.size(); i++) {
                Neighbor current = neighbors.get(i);
                if (current.getInterested() && current.getChoked()) {
                    candidatePool.add(current);
                }
            }

            // if we dont need to change opt unchoked neighbor (this happens when
            // neighbors.size() == 0 or !current.themInterested || !current.themChoked
            if (!candidatePool.isEmpty()) {
                if (optUnchoked != null) {
                    sendMessage(MessageType.CHOKE, optUnchoked.getOutputStream(), optUnchoked.getInputStream(),
                            optUnchoked.neighborID, -1);
                }

                optUnchoked = candidatePool.get(rand.nextInt(candidatePool.size()));
                logger.changeOptimisticallyUnchokedNeighbors(Integer.toString(optUnchoked.neighborID));

                // Send "UNCHOKE" message
                sendMessage(MessageType.UNCHOKE, optUnchoked.getOutputStream(), optUnchoked.getInputStream(),
                        optUnchoked.neighborID, -1);
            } else {
                // Handle the case where there are no valid candidates to choose from.
                logger.changeOptimisticallyUnchokedNeighbors("None");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void checkTermination() {
        AtomicBoolean t = new AtomicBoolean(true);
        completedPeerTracker.forEach((key, value) -> {
            if (!value) {
                t.set(false);
                return;
            }
        });

        if (t.get()) {
            System.out.println("All peers have the file, therefore terminating");
            terminate();
            System.exit(0);
        }
    }

    // might need to add synchronized
    public boolean receiveMessage(ObjectOutputStream out, ObjectInputStream in, int connectionID) {
        messenger.decodeMessage(out, in, connectionID);
        return true;
    }

    // syncronized was causing deadlock
    public boolean sendMessage(MessageType type, ObjectOutputStream out, ObjectInputStream in, int connectionID,
            int pieceIndex) {
        messenger.sendMessage(type, out, in, connectionID, pieceIndex);
        return true;
    }

    // Getters

    public boolean havePiece(int idx){
        for (Piece p: pieces) {
            if(p.getIndex() == idx){
                return p.isComplete();
            }
        }
        return false;
    }

    public Neighbor getPeer(int peerID) {
        for (int i = 0; i < neighbors.size(); i++) {
            if (neighbors.elementAt(i).neighborID == peerID) {

                return neighbors.get(i);
            }
        }

        System.out.println("Did not find a neighbor using the getPeer method");
        return null;
    }

    public PeerLogger getLogger() {
        return this.logger;
    }

}
