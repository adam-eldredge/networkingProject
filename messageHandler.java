import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


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

    private void handleHave(int peerID, int len) {
        // This function will handle a have message received
        peer.getLogger().receiveHave(Integer.toString(peerID), len);
    }

    private void handleBitfield(String peerBitfield, int peerID) {
        if (peerBitfield.length() == 0) {
            // Set connections bitfield to empty all zero
            Neighbor neighbor = peer.getPeer(peerID);
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
                    // Checking to see if they have a 1 and we have a 0
                    // Meaning they have a piece that we dont have so we are interested
                    interested = true;
                }
            }

            if (interested) {
                sendInterested();
            }
            else {
                sendUninterested();
            }
        }
        return;
    }

    private void handleRequest(int peerID, int len) {
        // This function will handle a request message received

    }

    private void handlePiece(int peerID, int len) {
        int pieceIndex = 0;
        int numPieces = 0;
        // This function will handle a piece message received
        
        
        peer.getLogger().downloadPiece(Integer.toString(peerID), pieceIndex, numPieces);

        //if and only if we have the complete file/all pieces
        peer.getLogger().completeDownload();
    }


    // sending message to peer
    public void sendMessage(MessageType type, String payload, ObjectOutputStream out, ObjectInputStream in, int peerID) {
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
    private void sendChoke(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00010";
        try {
            out.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendUnchoke(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00011";
        try {
            out.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendInterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        String msg = "00012";
        try {
            out.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
