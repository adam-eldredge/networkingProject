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
                    payload = msg.substring(65, (int) (65 + peer.pieceSize)).getBytes();
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

        if (numPieces() != bitFieldSize) {
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

        if (complete) {neighbor.hasFile = true;}

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

        savePiece(payload, index);

        // Find number of pieces after download
        int numPieces = numPieces();
        peer.getLogger().downloadPiece(Integer.toString(peerID), index, numPieces);
        for (int i = 0; i < peer.neighbors.size(); i++) {
            peer.sendMessage(MessageType.HAVE, peer.neighbors.elementAt(i).getOutputStream(), peer.neighbors.elementAt(i).getInputStream(), peer.neighbors.elementAt(i).neighborID, index);
        }

        if (numPieces == bitFieldSize) {
            // set hasFile to true
            peer.getLogger().completeDownload();
            peer.fileCompleted = true;

            // Send uninterested - to all of our connections that we were previously interested in
            for (int i = 0; i < peer.neighbors.size(); i++) {
                peer.sendMessage(MessageType.UNINTERESTED, peer.neighbors.elementAt(i).getOutputStream(), peer.neighbors.elementAt(i).getInputStream(), peer.neighbors.elementAt(i).neighborID, -1);
            }
        } else {
            Neighbor neighbor = peer.getPeer(peerID);

            // Figure out if still interested
            boolean interested = false;
            for (int i = 0; i < peer.bitfield.getBitSize(); i++) {
                if (neighbor.bitfield.hasPiece(i) && !(peer.bitfield.hasPiece(i))) {
                    interested = true;
                }
            }

            if (interested) {
                int reqPiece = randomRequestIndex(neighbor);
                peer.sendMessage(MessageType.REQUEST, out, in, peerID, reqPiece);
            }
            else {
                peer.sendMessage(MessageType.UNINTERESTED, out, in, peerID, -1);
            }
        }
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

    private int numPieces() {
        int count = 0;
        try {
            for (int i = 0; i < peer.filebytes.length; i++) {
                if (peer.filebytes[i] != 0)
                    count++;
            }
        } catch (Exception e) {
            System.out.println("Bad index in numPieces");
        }
        int total = count / (int) peer.pieceSize;
        if (count % (int) peer.pieceSize != 0)
            total++;
        return total;
    }

    private int randomRequestIndex(Neighbor neighbor) {
        // Randomly choose another piece that we don't have and havent requested
        Random rand = new Random();
        int reqIndex = -1;
        do {
            reqIndex = rand.nextInt(bitFieldSize);
            System.out.println("Requested Index: " + reqIndex);
            System.out.println(peer.bitfield.hasPiece(reqIndex) == true);
            System.out.println(neighbor.requestedIndices.contains(reqIndex));
        } while (peer.bitfield.hasPiece(reqIndex) == true || neighbor.requestedIndices.contains(reqIndex));

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
        int messageLength = 32 + (int) peer.pieceSize;
        try {
            // Message to write
            String msg = "";

            // Length
            msg += String.format("%32s", Integer.toBinaryString(1 + messageLength)).replace(" ", "0");

            // Type
            msg += "7";

            // Payload
            msg += String.format("%32s", Integer.toBinaryString(pieceIndex)).replace(" ", "0");

            int start = pieceIndex * (int) peer.pieceSize;
            int end = 0;
            if (pieceIndex == bitFieldSize - 1) // last piece
                end = peer.filebytes.length;
            else // not last piece (full piece)
                end = start + (int) peer.pieceSize;

            msg += (new String(peer.filebytes)).substring(start, end);

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
