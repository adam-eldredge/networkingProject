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
    public void decodeMessage(String msg, ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            //might be in bits format and needs to be converted into ints
            int length = Integer.parseInt(msg.substring(0,4));
            int type = Integer.parseInt(msg.substring(4,5));
            MessageType messageType = MessageType.values()[type];
            
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
                    handleHave(peerID, length);
                    break;
                case BITFIELD:
                    handleBitfield(msg.substring(6, 6 + length), peerID);
                    break;
                case REQUEST:
                    handleRequest(peerID, length);
                    break;
                case PIECE:
                    handlePiece(peerID, length);
                    break;
                default:
                    throw new RuntimeException("Invalid message type");
            }
           
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // *** MESSAGE HANDLING *** //
    private void handleChoke(int peerID) { 
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setUsChoked(true); 
        peer.getLogger().chokedNeighbor(Integer.toString(peerID));
    }

    private void handleUnchoke(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setUsChoked(false);
        peer.getLogger().unchokedNeighbor(Integer.toString(peerID));
    }

    private void handleInterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setUsInterested(true);
        peer.getLogger().receiveInterested(Integer.toString(peerID));
    }

    private void handleUninterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setUsInterested(false);
        peer.getLogger().receiveNotInterested(Integer.toString(peerID));
    }

    private void handleHave(int peerID, int index, ObjectOutputStream out, ObjectInputStream in) {
        // Check to see if we have the piece that we have received an index for, if not send an interested message and set us as interested
        Neighbor neighbor = peer.getPeer(peerID);

        // Update their bitfield
        neighbor.neighborBitfield[index] = 1;

        // Check to see if we are interested in that piece
        if (peer.bitfield[index] == 0) {
            neighbor.setUsInterested(true);

            // SEND INTERESTED MESSAGE HERE
            peer.sendMessage(MessageType.INTERESTED, null, out, in, peerID);
        }

        peer.getLogger().receiveHave(Integer.toString(peerID), index);
    }

    private void handleBitfield(String peerBitfield, int peerID, ObjectOutputStream out, ObjectInputStream in) {
        Neighbor neighbor = peer.getPeer(peerID);

        if (peerBitfield.length() == 0) {
            // Set connections bitfield to empty all zero
            neighbor.clearBitfield();
            return;
        }
        else {
            int[] receivedBitfield = new int[bitFieldSize];

            for (int i = 0; i < peerBitfield.length(); i++) {
                receivedBitfield[i] = peerBitfield.charAt(i);
            }

            // We need to compare the two bitfields and keep track of which bits we are interested in
            boolean interested = false;
            for (int i = 0; i < bitFieldSize; i++) {
                if ((receivedBitfield[i] - peer.bitfield[i]) == 1) {
                    interested = true;
                }
            }

            if (interested) {
                peer.sendMessage(MessageType.INTERESTED, null, out, in, peerID);
            }
            else {
                peer.sendMessage(MessageType.UNINTERESTED, null, out, in, peerID);
            }
        }
        return;
    }

    private void handleRequest(int peerID, int index, ObjectOutputStream out, ObjectInputStream in) {
        // This function will handle a request message received
        // Make sure we have the peice and send it
        if (peer.bitfield[index] == 0) return;

        // TODO - WHAT SHOULD THE PAYLOAD BE
        String payload = "hello";

        peer.sendMessage(MessageType.PIECE, payload, out, in, peerID);
    }

    private void handlePiece(int peerID, int index, String payload, ObjectOutputStream out, ObjectInputStream in) {
        // This function will handle a piece message received
        // Download piece here
        savePiece(payload, index);
        
        // Find number of pieces after download
        int numPieces = numPieces(peer.bitfield);

        peer.getLogger().downloadPiece(Integer.toString(peerID), index, numPieces);

        //if and only if we have the complete file/all pieces
        if (fullBitfield(peer.bitfield)) { peer.getLogger().completeDownload(); }

        // Randomly choose another piece that we don't have and havent requested
        Random rand = new Random();
        int reqIndex; 
        do {
            reqIndex = rand.nextInt(peer.bitFieldSize);
        } while (peer.bitfield[reqIndex] == 1);

        peer.sendMessage(MessageType.REQUEST, String.valueOf(reqIndex), out, in, peerID);
    }
    private boolean fullBitfield(int[] bitfield) {
        for (int i = 0; i < bitfield.length; i++) {
            if (bitfield[i] == 0) return false;
        }
        return true;
    }

    private void savePiece(String payload, int index) {
        // TODO - Save the payload into the correct index here
    }

    private int numPieces(int[] bitfield) {
        int count = 0;
        for (int i = 0; i < bitfield.length; i++) {
            if (bitfield[i] == 1) count ++;
        }

        return count;
    }

    // sending message to peer
    public void sendMessage(MessageType type, String payload, ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        // PeerID is who the message needs to go to
        switch(type) {
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
        String msg = "00010";
        try {
            out.writeBytes(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendUnchoke(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00011";
        try {
            out.writeBytes(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendInterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00012";
        try {
            out.writeBytes(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendUninterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00013";
        try {
            out.writeBytes(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHave(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        String msg = "00014";
        try {
            out.writeBytes(msg);
            out.writeInt(pieceIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }

    private void sendBitfield(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00015";
        try {
            out.writeBytes(msg);
            out.writeBytes(peer.getBitfieldString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRequest(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        String msg = "00016";
        try {
            out.writeBytes(msg);
            out.writeInt(pieceIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }   

    private void sendPiece(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        String msg = "00017";
        try {
            out.writeBytes(msg);
            out.writeInt(pieceIndex);
            out.write(peer.filebytes, pieceIndex, (int)peer.pieceSize);
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }
}
