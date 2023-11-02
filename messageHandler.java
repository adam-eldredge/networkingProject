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
    public void decodeMessage(String msg, ObjectOutputStream out, int peerID) {
        try {
            int length = Integer.parseInt(msg.substring(0,4));
            int type = Integer.parseInt(msg.substring(4,5));

            if (type == 0) {
                handleChoke(peerID);
            }
            else if (type == 1) {
                handleUnchoke(peerID);
            }
            else if (type == 2) {
                handleInterested(peerID);
            }
            else if (type == 3) {
                handleUninterested(peerID);
            }
            else if (type == 4) {
                handleHave(peerID, length);
            }
            else if (type == 5) {
                handleBitfield(msg.substring(6, 6 + length), peerID);
            }
            else if (type == 6) {
                handleRequest(peerID, length);
            }
            else if (type == 7) {
                // add data amount to calculate download rate

                handlePiece(peerID, length);
            }
            else {
                // Something is wrong
                return;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // *** MESSAGE HANDLING *** //
    public void handleChoke(int peerID) { 
        Connection neighbor = peer.getPeer(peerID);
        neighbor.setUsChoked(true); 
        peer.getLogger().chokedNeighbor(Integer.toString(peerID));
    }

    public void handleUnchoke(int peerID) {
        Connection neighbor = peer.getPeer(peerID);
        neighbor.setUsChoked(false);
        peer.getLogger().unchokedNeighbor(Integer.toString(peerID));
    }

    public void handleInterested(int peerID) {
        Connection neighbor = peer.getPeer(peerID);
        neighbor.setUsInterested(true);
        peer.getLogger().receiveInterested(Integer.toString(peerID));
    }

    public void handleUninterested(int peerID) {
        Connection neighbor = peer.getPeer(peerID);
        neighbor.setUsInterested(false);
        peer.getLogger().receiveNotInterested(Integer.toString(peerID));
    }

    public void handleHave(int peerID, int len) {
        // This function will handle a have message received
        peer.getLogger().receiveHave(Integer.toString(peerID), len);
    }

    public void handleBitfield(String peerBitfield, int peerID) {
        if (peerBitfield.length() == 0) {
            // Set connections bitfield to empty all zero
            Connection neighbor = peer.getPeer(peerID);
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

    public void handleRequest(int peerID, int len) {
        // This function will handle a request message received

    }

    public void handlePiece(int peerID, int len) {
        int pieceIndex = 0;
        int numPieces = 0;
        // This function will handle a piece message received
        
        
        peer.getLogger().downloadPiece(Integer.toString(peerID), pieceIndex, numPieces);

        //if and only if we have the complete file/all pieces
        peer.getLogger().completeDownload();
    }



    public void sendMessage(int type, String payload, ObjectOutputStream out, int peerID) {
        // PeerID is who the message needs to go to
        switch(type) {
            case 0:
                sendChoke();
                break;
            case 1:
                sendUnchoke();
                break;
            case 2:
                sendInterested();
                break;
            case 3:
                sendUninterested();
                break;
            case 4:
                sendHave(payload);
                break;
            case 5:
                sendBitfield();
                break;
            case 6:
                sendRequest(payload);
                break;
            case 7:
                sendPiece(payload);
                break;
            default:
                break;
        }
    }

    // *** MESSAGE SENDING *** //
    public void sendChoke() {
        String msg = "00010";
        //sendMessage(msg);
    }
    public void sendUnchoke() {
        String msg = "00011";
        //sendMessage(msg);
    }
    public void sendInterested() {
        String msg = "00012";
        //sendMessage(msg);
    }
    public void sendUninterested() {
        String msg = "00013";
        //sendMessage(msg);
    }

    public void sendHave(String payload) {
        // This function will send the have message with the necessary payload
    }

    public void sendBitfield() {
        //sendMessage(Arrays.toString(peer.bitfield));
    }

    public void sendRequest(String payload) {
        // This function will send the request message with the necessary payload
    }

    public void sendPiece(String payload) {
        // This function will send the piece message with the necessary payload
    }
}
