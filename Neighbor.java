import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

public class Neighbor {

    public int neighborID;
    public String hostName;
    public int portNum;
    public boolean hasFile;
    public Connection socketConnection;
    public ConnectionType type;
    private int numPieces = 0;

    // public Bitfield bitfield;
    public byte[] piecesIdxMap; 

    private boolean choked = true; // Do we have them choked
    private boolean Interested = false; // Them interested in us
    // public volatile Vector<Integer> requestedIndices = new Vector<>();

    public Neighbor(Connection connection, int peerID, boolean hasFile, ConnectionType type) {
        this.socketConnection = connection;
        this.neighborID = peerID;
        this.hasFile = hasFile;
        this.type = type;
    }

    public void setPiecesIdxMap(byte[] data) {
        this.piecesIdxMap = piecesIdxMap;
        setNumPieces();
    }
    private void setNumPieces(){
        int counter = 0;
        for (byte b : piecesIdxMap) {
            for (int i = 0; i < 8; i++) { // Assuming 8 bits per byte
                if ((b & (1 << (7 - i))) != 0) {
                    counter++;
                }
            }
        }
        this.numPieces = counter;
    };
    public int getNumPieces(){
        return numPieces;
    }
    public byte[] getPiecesIdxMap() {
        return piecesIdxMap;
    }
    public void updatePiecesIdxMap(int index) {
        //increment the number of pieces
        this.numPieces++;
        int counter = 0;
        for (byte b : piecesIdxMap) {
            for (int i = 0; i < 8; i++) { // Assuming 8 bits per byte
                if (counter == index) {
                    //change the bit to 1
                    b |= (1 << (7 - i));
                    return;
                }
                counter++;
            }
        }
    }

    // Setters
    public void setChoked(boolean c) {
        this.choked = c;
    }

    public void setInterested(boolean c) {
        this.Interested = c;
    }


    // Getters
    public Connection getConnection() {
        return socketConnection;
    }

    public ObjectOutputStream getOutputStream() {
        return socketConnection.getOutputStream();
    }

    public ObjectInputStream getInputStream() {
        return socketConnection.getInputStream();
    }

    public boolean getChoked() {
        return choked;
    }

    public boolean getInterested() {
        return Interested;
    }
}
