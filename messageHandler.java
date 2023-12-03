import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.sound.midi.Soundbank;

public class messageHandler {

    private int numPieces = 0;
    peerProcess peer = null;

    public void setNumPieces(int numPieces) {
        this.numPieces = numPieces;
    }

    public messageHandler(peerProcess peer) {
        this.peer = peer;
    }

    // decode message - returns message type with payload
    // peerID is who the message came from

    // receiving message from peer
    public void decodeMessage(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {

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

        if (peer.requestedIdxTracker.containsKey(peerID)) {
            peer.requestedIdxTracker.remove(peerID);
        }
    }

    private void handleUnchoke(int peerID) {
        peer.getLogger().unchokedNeighbor(Integer.toString(peerID));
        Neighbor neighbor = peer.neighborMap(peerID);

        if (!peer.fileCompleted) {
            int reqPiece = randRequestIdx(neighbor);
            // Send request
            peer.sendMessage(MessageType.REQUEST, neighbor.getOutputStream(), neighbor.getInputStream(), peerID,
                    reqPiece);
        }

    }

    private int randRequestIdx(Neighbor neighbor) {
        Random rand = new Random();
        int reqIndex = -1;
        boolean valid;

        do {
            reqIndex = rand.nextInt(numPieces);

            // Have we already requested this piece?
            boolean haveRequested = false;
            for (Integer key : peer.requestedIdxTracker.keySet()) {
                if (key == reqIndex) {
                    haveRequested = true;
                    break;
                }
            }

            // Do we already have the piece?
            boolean weHavePiece = doesPieceExist(reqIndex, peer.piecesIdxMap);

            // Do they have the piece?
            boolean theyHavePiece = doesPieceExist(reqIndex, neighbor.piecesIdxMap);
            
            valid = theyHavePiece && !weHavePiece && !haveRequested;

        } while (valid == false);

        peer.requestedIdxTracker.put(peerID, reqIndex);
        return reqIndex;
    }

    private boolean doesPieceExist(int index, byte[] data) {
        // Compare bitfields
        int counter = 0;
        for (byte b : data) {
            for (int i = 0; i < 8; i++) { // Assuming 8 bits per byte

                if (counter == index) {
                    // Create a mask for each bit position
                    byte mask = (byte) (1 << i);

                    // Check if the bit at position i is set
                    boolean isBitSet = (b & mask) != 0;
                    if (isBitSet) {
                        return true;
                    }
                }
                counter++;
            }
        }
        return false;
    }

    private void handleInterested(int peerID) {
        Neighbor neighbor = peer.neighborMap(peerID);
        neighbor.setInterested(true); // They are interested in us

        peer.getLogger().receiveInterested(Integer.toString(peerID));
    }

    private void handleUninterested(int peerID) {
        peer.getLogger().receiveNotInterested(Integer.toString(peerID));
        Neighbor neighbor = peer.neighborMap(peerID);
        neighbor.setInterested(false); // They are not interested in us
        // System.out.println("Peer " + peerID + " is Uninterested in us.");
    }

    private void handleHave(int peerID, ObjectInputStream in, ObjectOutputStream out, int index) {
        Neighbor neighbor = peer.neighborMap(peerID);

        neighbor.updatePiecesIdxMap(index);

        // checking if neighbor has all pieces
        boolean complete = neighbor.getNumPieces() == numPieces;

        if (complete) {
            peer.setCompletedPeer(neighbor.neighborID);
            neighbor.hasFile = true;
        }

        // Determine if interested
        boolean interested = areWeInterested(neighbor);

        if (interested) {
            peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
        }

        peer.getLogger().receiveHave(Integer.toString(peerID), index);
    }

    private void handleBitfield(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, byte[] payload) {
        Neighbor neighbor = peer.neighborMap(peerID);
        neighbor.setPiecesIdxMap(payload);

        // Compare bitfields
        boolean interested = areWeInterested(neighbor);

        if (interested) {
            peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
        } else {
            peer.sendMessage(MessageType.UNINTERESTED, out, in, peerID, -1);
        }
    }

    private void handleRequest(int peerID, ObjectInputStream in, ObjectOutputStream out, int index) {
        System.out.println("Index was " + index);
        if (peer.havePiece(index)) {
            System.out.println("We have the requested piece so should send");
            sendMessage(MessageType.PIECE, out, in, peerID, index);
        }
    }

    private void handlePiece(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, int index,
            byte[] payload) {

        if (peer.fileCompleted) {
            // Safety to not re download pieces
            return;
        }

        // Download the piece
        peer.setPiece(index, payload);

        // Find number of pieces after download
        peer.currentNumPieces++;

        peer.getLogger().downloadPiece(Integer.toString(peerID), index, peer.currentNumPieces);

        for (Neighbor neighbor : peer.neighborMap.values()) {
            MessageType type = MessageType.HAVE;
            ObjectOutputStream output = neighbor.getOutputStream();
            ObjectInputStream input = neighbor.getInputStream();
            int id = neighbor.neighborID;

            sendMessage(type, output, input, id, index);
        }
        peer.fileCompleted = (currentNumPieces == numPieces);

        if (peer.fileCompleted) {

            // FILE COMPLETE
            peer.getLogger().completeDownload();
            peer.setCompletedPeer(peer.ID);

            // SEND UNINTERESTED TO PEERS
            for (Neighbor neighbor : peer.neighborMap.values()) {
                MessageType type = MessageType.UNINTERESTED;
                ObjectOutputStream output = neighbor.getOutputStream();
                ObjectInputStream input = neighbor.getInputStream();
                int id = neighbor.neighborID;

                sendMessage(type, output, input, id, -1);
            }
        } else {

            // TODO: ask about logic below

            // Check if we are interested in other peers
            // If still interested in current neighbor, send request as well
            for (Neighbor neighbor : peer.neighborMap.values()) {

                // Message details
                ObjectOutputStream output = neighbor.getOutputStream();
                ObjectInputStream input = neighbor.getInputStream();
                int id = neighbor.neighborID;

                if (areWeInterested(neighbor) && id == peerID) {
                    int reqPiece = randRequestIdx(neighbor);
                    peer.sendMessage(MessageType.REQUEST, output, input, id, reqPiece);
                } else if (!areWeInterested(neighbor)) {
                    peer.sendMessage(MessageType.UNINTERESTED, output, input, id, -1);
                }
            }
        }
    }

    private boolean areWeInterested(Neighbor neighbor) {
        byte[] payload = neighbor.getPiecesIdxMap();

        // Compare bitfields
        int counter = 0;
        for (byte b : payload) {
            for (int i = 0; i < 8; i++) { // Assuming 8 bits per byte
                // Create a mask for each bit position
                byte mask = (byte) (1 << i);

                // Check if the bit at position i is set
                boolean isBitSet = (b & mask) != 0;
                if (isBitSet && peer.pieces[counter].isComplete() == false) {
                    return true;
                }
                counter++;
                // Output
                // System.out.println("Byte: " + b + ", Bit position: " + i + ", Bit value: " +
                // (isBitSet ? 1 : 0));
            }
        }
        return false;
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
        if (pieceIndex == numPieces - 1) // last piece
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
