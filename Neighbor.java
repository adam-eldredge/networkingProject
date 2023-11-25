import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Neighbor {

    public int neighborID; 
    public String hostName;
    public int portNum;
    public boolean hasFile;
    public Connection socketConnection;
    public ConnectionType type;
    public Bitfield bitfield;
    private boolean choked = true; // Do we have them choked
    private boolean Interested = false; // Them interested in us

    public Neighbor(Connection connection, int peerID, boolean hasFile, ConnectionType type) {
        this.socketConnection = connection;
        this.neighborID = peerID;
        this.hasFile = hasFile;
        this.type = type;
    }

    // Setters
    public void setChoked(boolean c) {
        this.choked = c;
    }
    public void setInterested(boolean c) {
        this.Interested = c;
    }


    public void updatePeerBitfield(Bitfield bitfield) {
        this.bitfield = bitfield;
    }

    public void clearBitfield() {
        for (int i = 0; i < bitfield.getByteSize(); i++) {
            bitfield.getData()[i] = 0;
        }
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
