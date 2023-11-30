import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import javax.sound.midi.Soundbank;

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

            // *** MESSAGE FORMAT
            // [0 - 31] : Binary representation of length
            // [32] : Integer representation (0-7) of type
            // [33 - x] : Payload

            // Read all necessary components

            String msg = (String) in.readObject();

            int length = Integer.parseInt(msg.substring(0, 32), 2);
            int type = Integer.parseInt(msg.substring(32, 33));

            int index = -1;
            byte[] payload = new byte[0];

            MessageType messageType = MessageType.values()[type];

            System.out.println(
                    "\n| ----- Received New Message -----" +
                            "\n| Type " + messageType +
                            "\n| From: " + peerID +
                            "\n| Length: " + length +
                            // "\n| Contents: " + msg +
                            "\n| --------------------------------");

            switch (messageType) {
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
                    index = Integer.parseInt(msg.substring(33, 65), 2);
                    handleHave(peerID, in, out, index);
                    break;
                case BITFIELD:
                    payload = msg.substring(33, 33 + length - 1).getBytes();
                    handleBitfield(peerID, length, in, out, payload);
                    break;
                case REQUEST:
                    index = Integer.parseInt(msg.substring(33, 65), 2);
                    handleRequest(peerID, in, out, index);
                    break;
                case PIECE:
                    index = Integer.parseInt(msg.substring(33, 65), 2);

                    payload = msg.substring(65, length - 33).getBytes();
                    handlePiece(peerID, length, in, out, index, payload);
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
        neighbor.requestedIndices.clear();
    }

    private void handleUnchoke(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        peer.getLogger().unchokedNeighbor(Integer.toString(peerID));

        if (peer.bitfield.getNumPieces() != bitFieldSize) {
            // Get random index to request
            neighbor.requestedIndices.clear();
            int reqPiece = randomRequestIndex(neighbor);

            // Send request
            peer.sendMessage(MessageType.REQUEST, neighbor.getOutputStream(), neighbor.getInputStream(), peerID,
                    reqPiece);
        }
    }

    private void handleInterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setInterested(true); // They are interested in us

        System.out.println("Peer " + peerID + " is Interested in us.");
        peer.getLogger().receiveInterested(Integer.toString(peerID));
    }

    private void handleUninterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setInterested(false); // They are not interested in us
        System.out.println("Peer " + peerID + " is Uninterested in us.");
        peer.getLogger().receiveNotInterested(Integer.toString(peerID));
    }

    private void handleHave(int peerID, ObjectInputStream in, ObjectOutputStream out, int index) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.bitfield.setPiece(index);

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

        // Compare bitfields
        boolean interested = false;
        for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
            if (b.hasPiece(i) && !(peer.bitfield.hasPiece(i))) {
                interested = true;
            }
        }

        if (interested) {
            peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
        } else {
            peer.sendMessage(MessageType.UNINTERESTED, out, in, peerID, -1);
        }
    }

    private void handleRequest(int peerID, ObjectInputStream in, ObjectOutputStream out, int index) {
        System.out.println("Index was " + index);
        if (peer.bitfield.hasPiece(index)) {
            System.out.println("We have the requested piece so should send");
            peer.sendMessage(MessageType.PIECE, out, in, peerID, index);
        }
    }

    private void handlePiece(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, int index,
            byte[] payload) {

        if (peer.fileCompleted == true) {
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

            // SEND UNINTERESTED TO PEERS
            for (int i = 0; i < peer.neighbors.size(); i++) {

                Neighbor connection = peer.neighbors.elementAt(i);

                // Message details
                MessageType type = MessageType.UNINTERESTED;
                ObjectOutputStream output = connection.getOutputStream();
                ObjectInputStream input = connection.getInputStream();
                int id = connection.neighborID;

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
            }
        }

        return interested;
    }

    private void savePiece(byte[] payload, int index) {
        try {
            peer.bitfield.setPiece(index);
            int start = index * (int) peer.pieceSize;
            int end = 0;
            if (index == bitFieldSize - 1) // last piece
                end = peer.filebytes.length;
            else // not last piece (full piece)
                end = start + (int) peer.pieceSize;

            for (int i = start, j = 0; i < end && j < payload.length; i++, j++) {
                peer.filebytes[i] = payload[j];
            }
        } catch (Exception e) {
            System.out.println("Bad index in savePiece");
        }

    }

    private int randomRequestIndex(Neighbor neighbor) {
        // Randomly choose another piece that we don't have and havent requested
        Random rand = new Random();
        int reqIndex = -1;
        boolean valid;
        do {
            reqIndex = rand.nextInt(bitFieldSize);
            valid = true;

            // Have we already requested this piece?
            for (int i = 0; i < peer.neighbors.size(); i++) {
                Neighbor current = peer.neighbors.elementAt(i);
                if (current.requestedIndices.contains(reqIndex)) {
                    valid = false;
                }
            }

            // Do we already have the piece?
            if (peer.bitfield.hasPiece(reqIndex) == true) {
                valid = false;
            }

        } while (valid == false);

        neighbor.requestedIndices.add(reqIndex);
        return (reqIndex);
    }

    // sending message to peer
    public void sendMessage(MessageType type, ObjectOutputStream out, ObjectInputStream in, int peerID,
            int pieceIndex) {
        // PeerID is who the message needs to go to

        System.out.println(
                "\n| ----- Sending New Message -----" +
                        "\n| Type " + type +
                        "\n| From: " + peerID +
                        "\n| -------------------------------");
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
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(1)).replace(" ", "0");

            // Type
            msg += "0";

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
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(1)).replace(" ", "0");

            // Type
            msg += "1";

            // Send message
            out.writeObject(msg);
            out.flush();

            Neighbor neighbor = peer.getPeer(peerID);
            neighbor.setChoked(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendInterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(1)).replace(" ", "0");

            // Type
            msg += "2";

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUninterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(1)).replace(" ", "0");
            ;

            // Type
            msg += "3";

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHave(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        try {
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(33)).replace(" ", "0");
            ;

            // Type
            msg += "4";

            // Payload
            msg += String.format("%32s", Integer.toBinaryString(pieceIndex)).replace(" ", "0");
            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendBitfield(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(peer.bitfield.getByteSize() + 1)).replace(" ", "0");
            ;

            // Type
            msg += "5";

            // Payload
            msg += new String(peer.bitfield.getData());

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRequest(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        try {
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(33)).replace(" ", "0");
            ;

            // Type
            msg += "6";

            // Payload
            msg += String.format("%32s", Integer.toBinaryString(pieceIndex)).replace(" ", "0");

            // Send message
            out.writeObject(msg);
            out.flush();
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

        int messageLength = 33 + (end - start);    

        try {
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(messageLength)).replace(" ", "0");

            // Type
            msg += "7";

            // Payload
            msg += String.format("%32s", Integer.toBinaryString(pieceIndex)).replace(" ", "0");

            msg += (new String(peer.filebytes)).substring(start, end);

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
