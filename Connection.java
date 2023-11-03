import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

public class Connection {
    public peerProcess us; // Our peerProcess

    public boolean handshakeCompleted = false;

    public int peerID; // Their ID
    public String hostName; // Their Hostname
    public int portNum; // Their portNum
    public boolean hasFile; // Do they have the file?

    // Bitfield information
    public int[] peerBitfield; // Their bitfield
    
    // State variables
    public boolean themChoked = false; // Us choking them
    public boolean usChoked = false; // Them choking us
    public boolean themInterested = false; // Them interested in us
    public boolean usInterested = false; // Us interested in them

    // Communication variables
    Socket socket; //socket connect to the server
    ObjectOutputStream out; //stream write to the socket
    ObjectInputStream in; //stream read from the socket
    String sendMessage; //message send to the server
    String receiveMessage; //capitalized message read from the server


    // CONSTRUCTOR
    public Connection(peerProcess us) {
        this.us = us;
    }
    public Connection(peerProcess us, int peerID, String hostName, int portNum, boolean hasFile) {
        this.us = us;
        this.peerID = peerID;
        this.hostName = hostName;
        this.portNum = portNum;
        this.hasFile = hasFile;
    }

/************************************************
 *************** SETTER FUNCTIONS ***************
 ************************************************/
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
    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
