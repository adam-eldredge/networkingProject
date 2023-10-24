import java.io.ObjectOutputStream;

public class messageHandler {

    int bitFieldSize = 10;
    peerProcess peer = null;

    public messageHandler(peerProcess peer, int bitFieldSize) {
        this.peer = peer;
        this.bitFieldSize = bitFieldSize;
    }

    // decode message - returns message type with payload
    public void decodeMessage(String msg, ObjectOutputStream out) {
        try {
            int length = Integer.parseInt(msg.substring(0,4));
            int type = Integer.parseInt(msg.substring(4,5));

            if (type == 0) {
                handleChoke();
            }
            else if (type == 1) {
                handleUnchoke();
            }
            else if (type == 2) {
                handleInterested();
            }
            else if (type == 3) {
                handleUninterested();
            }
            else if (type == 4) {
                handleHave(length);
            }
            else if (type == 5) {
                handleBitfield(msg.substring(6, 6 + length));
            }
            else if (type == 6) {
                handleRequest(length);
            }
            else if (type == 7) {
                handlePiece(length);
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
    public void handleChoke() { System.out.println("Handled the choke"); }

    public void handleUnchoke() {
        // This function will handle an unchoke message received
    }

    public void handleInterested() {
        // This function will handle an interested message received
    }

    public void handleUninterested() {
        // This function will handle an uninterested message received
    }

    public void handleHave(int len) {
        // This function will handle a have message received
    }

    public void handleBitfield(String peerBitfield) {
        if (peerBitfield.length() == 0) {
            // Set connections bitfield to empty all zero
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
                if ((receivedBitfield[i] + peer.bitfield[i]) % 2 == 1) {
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

    public void handleRequest(int len) {
        // This function will handle a request message received
    }

    public void handlePiece(int len) {
        // This function will handle a piece message received
    }



    public void sendMessage(int type, String payload, ObjectOutputStream out) {
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
