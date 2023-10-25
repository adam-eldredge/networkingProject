public class Connection {
    public int peerID;
    public String hostName;
    public int portNum;
    public boolean hasFile;

    // Bitfield information
    public int[] peerBitfield;
    
    // State variables
    public boolean themChoked = false; // Are they choked by us (not them to us)
    public boolean usChoked = false; // Are we choked by them (not them to us)
    public boolean themInterested = false;
    public boolean usInterested = false;
    public Client peerClient = null;

    public Connection(int peerID, String hostName, int portNum, boolean hasFile) {
        this.peerID = peerID;
        this.hostName = hostName;
        this.portNum = portNum;
        this.hasFile = hasFile;
    }

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

    public void updatePeerBitfield(int[] bitfield) {
        this.peerBitfield = bitfield;
    }

    public void clearBitfield() {
        for (int i = 0; i < peerBitfield.length; i++) {
            peerBitfield[i] = 0;
        }
    }
}
