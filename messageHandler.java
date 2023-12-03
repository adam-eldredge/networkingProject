import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Random;

public class messageHandler {

    int bitFieldSize = 0;
    peerProcess peer = null;

    public messageHandler(peerProcess peer) {
        this.peer = peer;
    }

    // decode message - returns message type with payload
    // peerID is who the message came from

    // receiving message from peer
    public void decodeMessage(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

            // Read all necessary components

            Object obj = in.readObject();
            System.out.println("Read in an object");

            Message msg = null;

            if (obj instanceof Message) {
                msg = (Message) obj;
            } else {
                return;
            }

            System.out.println("Object was valid. Type: " + msg.type);

            MessageType type = msg.type;

            int length = msg.length;
            switch (type) {
                case CHOKE:
                    handleChoke(peerID);
                    break;
                case UNCHOKE:
                    handleUnchoke(peerID);
                    break;
                case INTERESTED:
                    handleInterested(peerID);
                    break;
                case UNINTERESTED:
                    handleUninterested(peerID);
                    break;
                case HAVE:
                    handleHave(peerID, in, out, msg.index);
                    break;
                case BITFIELD:
                    handleBitfield(peerID, length, in, out, msg.payload);
                    break;
                case REQUEST:
                    handleRequest(peerID, in, out, msg.index);
                    break;
                case PIECE:
                    handlePiece(peerID, length, in, out, msg.index, msg.payload);
                    break;
                default:
                    throw new RuntimeException("Invalid message type");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // *** MESSAGE HANDLING *** //
    private void handleChoke(int peerID) {
        peer.getLogger().chokedNeighbor(Integer.toString(peerID));

        Neighbor neighbor = peer.getPeer(peerID);
        peer.bitfield.clearReqIdx(neighbor.neighborID);
    }

    private void handleUnchoke(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        peer.getLogger().unchokedNeighbor(Integer.toString(peerID));

        if (peer.bitfield.getNumPieces() != bitFieldSize) {
            int reqPiece = randomRequestIndex(neighbor);

            if (reqPiece == -1) {
                return;
            }

            // Send request
            peer.sendMessage(MessageType.REQUEST, neighbor.getOutputStream(), neighbor.getInputStream(), peerID,
                    reqPiece);
        }
    }

    private void handleInterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setInterested(true); // They are interested in us

        peer.getLogger().receiveInterested(Integer.toString(peerID));
    }

    private void handleUninterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setInterested(false); // They are not interested in us
        // loop and display all values in completedPeerTracker
        for (Map.Entry<Integer, Boolean> entry : peer.completedPeerTracker.entrySet()) {
            Integer key = entry.getKey();
            Boolean value = entry.getValue();

            // Do something with key and value
        }
        peer.getLogger().receiveNotInterested(Integer.toString(peerID));
    }

    private void handleHave(int peerID, ObjectInputStream in, ObjectOutputStream out, int index) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.bitfield.setPiece(index);

        //System.out.println("peer: "+ peerID + " has file: " + neighbor.hasFile);

        boolean complete = true;
        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (neighbor.bitfield.hasPiece(i) == false) {
                complete = false;
            }
        }

        if (complete) {
            int currentPeerID = neighbor.neighborID;
            peer.setCompletedPeer(currentPeerID);
            neighbor.hasFile = true;
        }

        // System.out.println("peer: "+ peerID + " has file: " + neighbor.hasFile);

        // Determine if interested
        boolean interested = false;
        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (neighbor.bitfield.hasPiece(i) && !(peer.bitfield.hasPiece(i))) {
                interested = true;
            }
        }

        if (interested) {
            peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
        }

        peer.getLogger().receiveHave(Integer.toString(peerID), index);
    }

    private void handleBitfield(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, byte[] payload) {
        Bitfield b = new Bitfield(payload);
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.bitfield = b;

        boolean complete = true;
        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (neighbor.bitfield.hasPiece(i) == false) {
                complete = false;
            }
        }

        if (complete) {
            int currentPeerID = neighbor.neighborID;
            peer.setCompletedPeer(currentPeerID);
            neighbor.hasFile = true;
        }

        // Compare bitfields
        boolean interested = false;
        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (b.hasPiece(i) && !(peer.bitfield.hasPiece(i))) {
                interested = true;
                break;
            }
        }

        if (interested) {
            peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
        } else {
            peer.sendMessage(MessageType.UNINTERESTED, out, in, peerID, -1);
        }
    }

    private void handleRequest(int peerID, ObjectInputStream in, ObjectOutputStream out, int index) {
        if (peer.bitfield.hasPiece(index)) {

            System.out.println("  ------------ We have the piece, sending piece: " + index + " to peerId: " + peerID);
            peer.sendMessage(MessageType.PIECE, out, in, peerID, index);
        } else {
            System.out.println("******************************* We dont have the piece");
        }
    }

    private void handlePiece(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, int index,
            byte[] payload) {

        if (peer.fileCompleted) {
            // Safety to not re download pieces
            return;
        }

        // Download the piece
        savePiece(payload, index);

        // Find number of pieces after download
        int numPieces = peer.bitfield.getNumPieces();

        peer.getLogger().downloadPiece(Integer.toString(peerID), index, numPieces);

        // Send HAVE to all neighbors
        for (int i = 0; i < peer.neighbors.size(); i++) {

            Neighbor connection = peer.neighbors.elementAt(i);

            // Message details
            MessageType type = MessageType.HAVE;
            ObjectOutputStream output = connection.getOutputStream();
            ObjectInputStream input = connection.getInputStream();
            int id = connection.neighborID;

            peer.sendMessage(type, output, input, id, index);
        }

        if (peer.bitfield.checkFull()) {

            // FILE COMPLETE
            peer.getLogger().completeDownload();
            peer.fileCompleted = true;
            peer.setCompletedPeer(peer.ID);

            // TODO: can be optomized to send only to peers that we were interested
            // SEND UNINTERESTED TO PEERS
            for (int i = 0; i < peer.neighbors.size(); i++) {

                Neighbor connection = peer.neighbors.elementAt(i);

                // Message details
                MessageType type = MessageType.UNINTERESTED;
                ObjectOutputStream output = connection.getOutputStream();
                ObjectInputStream input = connection.getInputStream();
                int id = connection.neighborID;

                System.out.println("ran the end code");
                peer.sendMessage(type, output, input, id, -1);
            }
        } else {
            // Current Neighbor
            Neighbor neighbor = peer.getPeer(peerID);

            // Check if we are interested in other peers
            // If still interested in current neighbor, send request as well
            for (int i = 0; i < peer.neighbors.size(); i++) {

                // Neighbor details
                Neighbor connection = peer.neighbors.elementAt(i);

                // Message details
                ObjectOutputStream output = connection.getOutputStream();
                ObjectInputStream input = connection.getInputStream();
                int id = connection.neighborID;

                if (areWeInterested(connection) && connection == neighbor) {
                    int reqPiece = randomRequestIndex(neighbor);
                    if (reqPiece == -1) {
                        return;
                    }
                    peer.sendMessage(MessageType.REQUEST, output, input, id, reqPiece);
                } else if (areWeInterested(peer.neighbors.elementAt(i)) == false) {
                    peer.sendMessage(MessageType.UNINTERESTED, output, input, id, -1);
                }
            }
        }
    }

    private boolean areWeInterested(Neighbor neighbor) {
        boolean interested = false;

        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (neighbor.bitfield.hasPiece(i) && !(peer.bitfield.hasPiece(i))) {
                interested = true;
                break;
            }
        }

        return interested;
    }

    private void savePiece(byte[] payload, int index) {
        try {
            // index = piece index

            System.out.println(" ------------ Downloading piece" + index);
            System.out.println(" ------------ Total piece downloaded: " + (peer.bitfield.getNumPieces() + 1));

            int start = index * (int) peer.pieceSize;
            int end = 0;
            if (index == bitFieldSize - 1) // last piece
                end = peer.filebytes.length;
            else // not last piece (full piece)
                end = start + (int) peer.pieceSize;

            for (int i = start, j = 0; i < end && j < payload.length; i++, j++) {
                peer.filebytes[i] = payload[j];
            }
            peer.bitfield.setPiece(index);
        } catch (Exception e) {
            System.out.println("Bad index in savePiece");
        }

    }

    private int randomRequestIndex(Neighbor neighbor) {
        // Randomly choose another piece that we don't have and havent requested
        Random rand = new Random();
        int reqIndex = -1;

        // Are we done/ requested all pieces?
        boolean makeAReq = false;
        System.out.println(peer.bitfield.getBitSize());
        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (peer.bitfield.hasPiece(i) == false || alreadyRequested(i) == false) {
                makeAReq = true;
            }
        }
        if (makeAReq == false) {
            System.out.println("We shouldnt need to request it");
            
            return -1;
        } else {
            do {
                reqIndex = rand.nextInt(bitFieldSize);
            } while (peer.bitfield.hasPiece(reqIndex) == true || alreadyRequested(reqIndex));

            peer.bitfield.addReqIdx(neighbor.neighborID, reqIndex);
            return (reqIndex);
        }
    }

    public boolean alreadyRequested(int index) {
        return peer.bitfield.requested(index);
    }

    // sending message to peer
    public synchronized void sendMessage(MessageType type, ObjectOutputStream out, ObjectInputStream in, int peerID,
            int pieceIndex) {
        // PeerID is who the message needs to go to

        switch (type) {
            case CHOKE:
                sendChoke(out, in, peerID);
                break;
            case UNCHOKE:
                sendUnchoke(out, in, peerID);
                break;
            case INTERESTED:
                sendInterested(out, in, peerID);
                break;
            case UNINTERESTED:
                sendUninterested(out, in, peerID);
                break;
            case HAVE:
                sendHave(out, in, peerID, pieceIndex);
                break;
            case BITFIELD:
                sendBitfield(out, in, peerID);
                break;
            case REQUEST:
                sendRequest(out, in, peerID, pieceIndex);
                break;
            case PIECE:
                sendPiece(out, in, peerID, pieceIndex);
                break;
            default:
                break;
        }

    }

    // *** MESSAGE SENDING *** //
    private void sendChoke(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            Message msg = new Message(MessageType.CHOKE);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;

            // Send message
            out.writeObject(msg);
            out.flush();

            Neighbor neighbor = peer.getPeer(peerID);
            neighbor.setChoked(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUnchoke(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

            Message msg = new Message(MessageType.UNCHOKE);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;
            // Send message
            out.writeObject(msg);
            out.flush();

            Neighbor neighbor = peer.getPeer(peerID);
            neighbor.setChoked(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendInterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

            Message msg = new Message(MessageType.INTERESTED);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUninterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

            Message msg = new Message(MessageType.UNINTERESTED);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHave(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        try {

            Message msg = new Message(MessageType.HAVE);
            msg.length = 1 + Integer.SIZE;
            msg.toID = peerID;
            msg.fromID = peer.ID;
            msg.index = pieceIndex;
            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBitfield(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

            Message msg = new Message(MessageType.BITFIELD);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;
            msg.payload = peer.bitfield.getData();

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRequest(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        try {

            Message msg = new Message(MessageType.REQUEST);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;
            msg.index = pieceIndex;

            // Send message
            out.writeObject(msg);
            out.flush();

            System.out.println("Requested Piece: " + pieceIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendPiece(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {

        int start = pieceIndex * (int) peer.pieceSize;
        int end = 0;
        if (pieceIndex == bitFieldSize - 1) // last piece
            end = peer.filebytes.length;
        else // not last piece (full piece)
            end = start + (int) peer.pieceSize;

        try {

            Message msg = new Message(MessageType.PIECE);
            msg.length = 1;
            msg.toID = peerID;
            msg.fromID = peer.ID;
            msg.index = pieceIndex;

            byte[] b = new byte[end - start];
            for (int i = start; i < end; i++) {
                b[i - start] = peer.filebytes[i];
            }
            msg.payload = b;

            // Send message

            out.writeObject(msg);
            out.flush();
            System.out.println("Wrote the piece to stream: " + msg.index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
