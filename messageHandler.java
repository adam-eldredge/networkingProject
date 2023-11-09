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
                    handleHave(peerID, Integer.parseInt(msg.substring(5, 9)), out, in);
                    break;
                case BITFIELD:
                    handleBitfield(msg.substring(5, 5 + length - 1), peerID, out, in);
                    break;
                case REQUEST:
                    handleRequest(peerID, Integer.parseInt(msg.substring(5, 9)), out, in);
                    break;
                case PIECE:
                    int index = Integer.parseInt(msg.substring(5,9));
                    String payload = msg.substring(9, 9 + length - 5);
                    handlePiece(peerID, index, payload, out, in);
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
        neighbor.setThemInterested(true);
        peer.getLogger().receiveInterested(Integer.toString(peerID));
    }

    private void handleUninterested(int peerID) {
        Neighbor neighbor = peer.getPeer(peerID);
        neighbor.setThemInterested(false);
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
            sendMessage(MessageType.INTERESTED, null, out, in, peerID);
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
                sendMessage(MessageType.INTERESTED, null, out, in, peerID);
            }
            else {
                sendMessage(MessageType.UNINTERESTED, null, out, in, peerID);
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

        sendMessage(MessageType.PIECE, payload, out, in, peerID);
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

        sendMessage(MessageType.REQUEST, String.valueOf(reqIndex), out, in, peerID);
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
    public void sendMessage(MessageType type, String payload, ObjectOutputStream out, ObjectInputStream in, int peerID) {
        // PeerID is who the message needs to go to
        switch(type) {
            case CHOKE:
                sendChoke();
                break;
            case UNCHOKE:
                sendUnchoke();
                break;
            case INTERESTED:
                sendInterested();
                break;
            case UNINTERESTED:
                sendUninterested();
                break;
            case HAVE:
                sendHave(payload);
                break;
            case BITFIELD:
                sendBitfield();
                break;
            case REQUEST:
                sendRequest(payload);
                break;
            case PIECE:
                sendPiece(payload);
                break;
            default:
                break;
        }
    }

    // *** MESSAGE SENDING *** //
    private void sendChoke() {
        String msg = "00010";
        //sendMessage(msg);
    }
    private void sendUnchoke() {
        String msg = "00011";
        //sendMessage(msg);
    }
    private void sendInterested() {
        String msg = "00012";
        //sendMessage(msg);
    }
    private void sendUninterested() {
        String msg = "00013";
        //sendMessage(msg);
    }

    private void sendHave(String payload) {
        // This function will send the have message with the necessary payload
    }

    private void sendBitfield() {
        //sendMessage(Arrays.toString(peer.bitfield));
    }

    private void sendRequest(String payload) {
        // This function will send the request message with the necessary payload
    }

    private void sendPiece(String payload) {
        // This function will send the piece message with the necessary payload
    }
}
