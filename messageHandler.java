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
            int length = in.readInt();
            int type = in.readByte();

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
                    handleHave(peerID, in, out);
                    break;
                case BITFIELD:
                    handleBitfield(peerID, length, in, out);
                    break;
                case REQUEST:
                    handleRequest(peerID, in, out);
                    break;
                case PIECE:
                    handlePiece(peerID, length, in, out);
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

    private void handleHave(int peerID, ObjectInputStream in, ObjectOutputStream out) {
        try{
            //Read 4 byte index
            int index = in.readInt();

            // Check to see if we have the piece that we have received an index for, if not send an interested message and set us as interested
            Neighbor neighbor = peer.getPeer(peerID);

            // Update their bitfield
            neighbor.bitfield.getData()[index] = 1;

            // Check to see if we are interested in that piece
            if (peer.bitfield.hasPiece(index)) {
                neighbor.setUsInterested(true);

                // SEND INTERESTED MESSAGE HERE
                peer.sendMessage(MessageType.INTERESTED, out, in, peerID, index);
            }

            peer.getLogger().receiveHave(Integer.toString(peerID), index);

        }catch (IOException e) {
            System.out.println("Bad index in handleHave");
        }
    }

    private void handleBitfield(int peerID, int length, ObjectInputStream in, ObjectOutputStream out) {
        try{
        Neighbor neighbor = peer.getPeer(peerID);
        byte[] payload = in.readNBytes(length);

        neighbor.bitfield.setData(payload);

        // if (peerBitfield.length() == 0) {
        //     // Set connections bitfield to empty all zero
        //     neighbor.clearBitfield();
        //     return;
        // }
        // else {
            int[] receivedBitfield = new int[bitFieldSize];

            for (int i = 0; i < peer.bitfield.getSize(); i++) {
                receivedBitfield[i] = peer.bitfield.getData()[i];
            }

            // We need to compare the two bitfields and keep track of which bits we are interested in
            boolean interested = false;
            for (int i = 0; i < bitFieldSize; i++) {
                if ((receivedBitfield[i] - peer.bitfield.getData()[i]) == 1) {
                    interested = true;
                }
            }

            if (interested) {
                peer.sendMessage(MessageType.INTERESTED, out, in, peerID, -1);
            }
            else {
                peer.sendMessage(MessageType.UNINTERESTED, out, in, peerID, -1);
            }
        // }
        }catch (IOException e) {
            System.out.println("Bad index in handleHave");
        }
    }

    private void handleRequest(int peerID, ObjectInputStream in, ObjectOutputStream out) {
        // This function will handle a request message received
        // Make sure we have the peice and send it
        try {
            int index = in.readInt();
            
            if (peer.bitfield.hasPiece(index)){
                peer.sendMessage(MessageType.PIECE, out, in, peerID, index);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
        private void handlePiece(int peerID, int length, ObjectInputStream in, ObjectOutputStream out) {
            // This function will handle a piece message received
        // Download piece here
        try {
            int index = in.readInt();
            byte[] payload = in.readNBytes(length);
            savePiece(payload, index);

            // Find number of pieces after download
            int numPieces = numPieces();
    
            peer.getLogger().downloadPiece(Integer.toString(peerID), index, numPieces);
    
            //if and only if we have the complete file/all pieces
            if (fullBitfield()) { peer.getLogger().completeDownload(); }
    
            // Randomly choose another piece that we don't have and havent requested
            Random rand = new Random();
            int reqIndex; 
            do {
                reqIndex = rand.nextInt(peer.bitFieldSize);
            } while (peer.bitfield.getData()[reqIndex] == 1);
    
            peer.sendMessage(MessageType.REQUEST, out, in, peerID, reqIndex);

        } catch (IOException e) {
            e.printStackTrace();
        }

      
        
    }
    private boolean fullBitfield() {
        if (numPieces() == peer.filebytes.length){
            return true;
        }
        else return false;
    }

    private void savePiece(byte[] payload, int index) {
        for (int i = index; i < payload.length; i++)
        {
            peer.filebytes[i] = payload[i];
        }
    }

    private int numPieces() {
        int count = 0;
        for (int i = 0; i < peer.filebytes.length; i++) {
            if (peer.filebytes[i] != 0) count ++;
        }

        return count;
    }

    // sending message to peer
    public void sendMessage(MessageType type, ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
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
        try {
            out.writeInt(0);
            out.writeByte(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendUnchoke(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            out.writeInt(0);
            out.writeByte(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendInterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            out.writeInt(0);
            out.writeByte(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendUninterested(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            out.writeInt(0);
            out.writeByte(3);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHave(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        try {
            out.writeInt(4);
            out.writeByte(4);
            out.writeInt(pieceIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }

    private void sendBitfield(ObjectOutputStream out, ObjectInputStream in, int peerID) {
        try {
            out.writeInt(peer.bitfield.getSize());
            out.writeByte(5);
            out.write(peer.bitfield.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRequest(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        try {
            out.writeInt(4);
            out.writeByte(6);
            out.writeInt(pieceIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }   

    private void sendPiece(ObjectOutputStream out, ObjectInputStream in, int peerID, int pieceIndex) {
        int messageLength = 4 + (int) peer.pieceSize;
        try {
            out.writeInt(messageLength);
            out.writeByte(7);
            out.writeInt(pieceIndex);
            out.write(peer.filebytes, pieceIndex, (int)peer.pieceSize);
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }
}
