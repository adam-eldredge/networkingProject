import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

public class messageHandler {

    int bitFieldSize = 10;
    peerProcess peer = null;

    public messageHandler(peerProcess peer, int bitFieldSize) {
        this.peer = peer;
        this.bitFieldSize = bitFieldSize;
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

            String msg = (String)in.readObject();

            int length = Integer.parseInt(msg.substring(0,32), 2);
            int type = Integer.parseInt(msg.substring(32,33));

            int index = -1;
            byte[] payload = new byte[0];

            MessageType messageType = MessageType.values()[type];
            switch (messageType) {
                case HAVE:
                    index = Integer.parseInt(msg.substring(5,9), 2);
                    break;
                case BITFIELD:
                    payload = msg.substring(33, 33 + length - 1).getBytes();
                    break;
                case REQUEST:
                    index = Integer.parseInt(msg.substring(33,37), 2);
                    break;
                case PIECE:
                    index = Integer.parseInt(msg.substring(33,37), 2);
                    payload = msg.substring(33, 33 + length - 1).getBytes();
                    break;
                default:
                    break;
            }

            System.out.println("Received: " + messageType + "   From: " + peerID + "\n      Length: " + length);

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
                    handleHave(peerID, in, out, index);
                    break;
                case BITFIELD:
                    handleBitfield(peerID, length, in, out, payload);
                    break;
                case REQUEST:
                    handleRequest(peerID, in, out, index);
                    break;
                case PIECE:
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
        // Neighbor neighbor = peer.getPeer(peerID);
        // neighbor.setChoked(true);
        // peer.getLogger().chokedNeighbor(Integer.toString(peerID));

        // DOES THIS NEED TO DO ANYTHING????
    }

    private void handleUnchoke(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        // neighbor.setChoked(false);
        // peer.getLogger().unchokedNeighbor(Integer.toString(peerID));

        // Get random index to request
        int reqPiece = randomRequestIndex();

        // Send request
        peer.sendMessage(MessageType.REQUEST, neighbor.getOutputStream(), neighbor.getInputStream(), peerID, reqPiece);
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
        neighbor.bitfield.getData()[index] = 1;

        // Check to see if we are interested in that piece
        if (peer.bitfield.hasPiece(index)) {
            neighbor.setInterested(true);
            peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
        }

        peer.getLogger().receiveHave(Integer.toString(peerID), index);
    }

    private void handleBitfield(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, byte[] payload) {
        Bitfield b = new Bitfield(payload);

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
        if (peer.bitfield.hasPiece(index)) {
            peer.sendMessage(MessageType.PIECE, out, in, peerID, index);
        }
    }

    private void handlePiece(int peerID, int length, ObjectInputStream in, ObjectOutputStream out, int index, byte[] payload) {
            
            savePiece(payload, index);

            // Find number of pieces after download
            int numPieces = numPieces();

            peer.getLogger().downloadPiece(Integer.toString(peerID), index, numPieces);

            // if and only if we have the complete file/all pieces
            if (fullBitfield()) {
                peer.getLogger().completeDownload();
            }

            int reqPiece = randomRequestIndex();

            peer.sendMessage(MessageType.REQUEST, out, in, peerID, reqPiece);
    }

    private boolean fullBitfield() {
        if (numPieces() == peer.filebytes.length) {
            return true;
        } else
            return false;
    }

    private void savePiece(byte[] payload, int index) {
        try {
            for (int i = index; i < payload.length; i++) {
                peer.filebytes[i] = payload[i];
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
        return count;
    }

    private int randomRequestIndex() {
        // Randomly choose another piece that we don't have and havent requested
        Random rand = new Random();
        int reqIndex = -1;
        do {
            reqIndex = rand.nextInt(peer.bitFieldSize);
        } while (peer.bitfield.hasPiece(reqIndex) == true || peer.requestedIndices.contains(reqIndex));

        peer.requestedIndices.add(reqIndex);
        return (reqIndex);
    }

    // sending message to peer
    public void sendMessage(MessageType type, ObjectOutputStream out, ObjectInputStream in, int peerID,
            int pieceIndex) {
        // PeerID is who the message needs to go to

        System.out.println("Sending     : " + type+ "   to: " + peerID + "\n");
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
            System.out.println("Just choked peer: " + peerID);

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
            System.out.println("Just choked peer: " + peerID);
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
            msg += String.format("%32s", Integer.toBinaryString(1)).replace(" ", "0");;

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
            msg += String.format("%32s", Integer.toBinaryString(33)).replace(" ", "0");;

            // Type
            msg += "4";

            // Payload
            msg += Integer.toBinaryString(pieceIndex);

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
            msg += String.format("%32s", Integer.toBinaryString(peer.bitfield.getByteSize() + 1)).replace(" ", "0");;

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
            msg += String.format("%32s", Integer.toBinaryString(33)).replace(" ", "0");;

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

            msg += (new String(peer.filebytes)).substring(pieceIndex, (int) peer.pieceSize);

            // Send message
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
