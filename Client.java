import java.net.*;
import java.io.*;

public class Client extends Connection {

    String message; // message send to the server
    String MESSAGE; // capitalized message read from the server
    String hostName;
    int portNum;
    int neighborID;
    peerProcess peer; // Parent peer of this client
    String header = "P2PFILESHARINGPROJ";
    String zeros = "0000000000";
    boolean hasFile = false;

    public Client(peerProcess p, String hostName, int portNum, int connectionID, boolean hasFile) {
        super(hostName, portNum);
        peer = p;
        this.hostName = hostName;
        this.portNum = portNum;
        this.neighborID = connectionID;
        this.hasFile = hasFile;

        // Create a new neighbor for this connection
        peer.neighbors.add(new Neighbor(this, connectionID, hasFile, ConnectionType.CLIENT));
    }

    @Override
    public void run() {
        try {
            sendHandshakeMessage();

            this.peer.getLogger().generateTCPLogSender(Integer.toString(neighborID));
            System.out.println("Connected to " + hostName + " in port " + portNum);

            // Wait for handshake response from server
            String handshakeResponse = (String) in.readObject();
            System.out.println("Received handshake Response: " + handshakeResponse);

            // Verify Handshake Response
            verifyHandshakeResponse(handshakeResponse);
            System.out.println("Handshake verified.");

            // createdNeighbor = neighbor;

            // Send bitfield
            peer.sendMessage(MessageType.BITFIELD, out, in, neighborID, -1);

            while (true) {
                peer.receiveMessage(out, in, neighborID);
            }
        } catch (Exception classnot) {
            System.err.println("Data received in unknown format");
        } finally {
            // Close connections
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void sendHandshakeMessage() {
        String id = String.valueOf(peer.ID);
        String msg = this.header + this.zeros + id;
        sendMessage(msg);
    }

    // Handshake response verification
    private void verifyHandshakeResponse(String msg) {
        String Header = msg.substring(0, 18);
        String zero = msg.substring(18, 28);
        int receivedID = Integer.parseInt(msg.substring(28, 32));

        if (!Header.equals(this.header) || !zero.equals(this.zeros) || this.neighborID != receivedID) {
            throw new RuntimeException();
        }

    }

}
