import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Neighbor {

    public int neighborID; 
    public String hostName;
    public int portNum;
    public boolean hasFile;
    public Client peerClient;

    // Bitfield information
    public Bitfield bitfield;
    
    // State variables
    public boolean themChoked = true; // Are they choked by us (not them to us)
    public boolean usChoked = false; // Are we choked by them (not them to us)
    public boolean themInterested = false;
    public boolean usInterested = false;

    // Messaging Information
    ObjectInputStream in;
    ObjectOutputStream out;

    public Neighbor(peerProcess peerProcessIntance, int peerID, boolean hasFile, ObjectInputStream in, ObjectOutputStream out) {
        this.neighborID = peerID;
        this.hasFile = hasFile;
        this.in = in;
        this.out = out;
    }

    // Setters

    public void setThemChoked(boolean c) {
        this.themChoked = c;
    }

    public void setUsChoked(boolean c) {
        this.usChoked = c;
    }

    public void setThemInterested(boolean c) {
        this.themInterested = c;
    }

    public void setUsInterested(boolean c) {
        this.usInterested = c;
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
    public Client getClient() {
        return peerClient;
    }

    public ObjectOutputStream getOutputStream() {
        return out;
    }
    public ObjectInputStream getInputStream() {
        return in;
    }
}
