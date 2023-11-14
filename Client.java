import java.net.*;
import java.io.*;

public class Client {

    Socket requestSocket; //socket connect to the server
    ObjectOutputStream out; //stream write to the socket
    ObjectInputStream in; //stream read from the socket
    String message; //message send to the server
    String MESSAGE; //capitalized message read from the server
    String hostName;
    int portNum;
    int neighborID; 
    peerProcess peer; // Parent peer of this client
    String header = "P2PFILESHARINGPROJ";
    String zeros = "0000000000";


    public Client(peerProcess p, String hostName, int portNum, int connectionID) {
        peer = p;
        this.hostName = hostName;
        this.portNum = portNum;
        this.neighborID = connectionID;
    }

    public void startConnection(){
        try{
            requestSocket = new Socket("localhost", portNum);
            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            try{ 
                sendHandshakeMessage();

                this.peer.getLogger().generateTCPLogSender(Integer.toString(neighborID));
                System.out.println("Connected to " + hostName + " in port " + portNum);
                
                //Wait for handshake response from server
                String handshakeResponse = (String)in.readObject();
                System.out.println("Received handshake Response: " + handshakeResponse);

                //Verify Handshake Response
                verifyHandshakeResponse(handshakeResponse);
                System.out.println("Handshake verified.");
                

            }catch(Exception classnot){
                System.err.println("Data received in unknown format");
            }
        }
        catch (ConnectException e) {
            System.err.println("Connection refused. You need to initiate a server first.");
        }
        catch(UnknownHostException unknownHost){
            System.err.println("You are trying to connect to an unknown host!");
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }
    

    public void closeConnection() {
        try {
            in.close();
            out.close();
            requestSocket.close();
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void sendHandshakeMessage() {
        String id = String.valueOf(peer.ID);
        String msg = this.header + this.zeros + id;
        sendMessage(msg);
    }

    // Handshake response verification
    private void verifyHandshakeResponse(String msg) {
        String Header = msg.substring(0,18);
        String zero = msg.substring(18,28); 
        int receivedID = Integer.parseInt(msg.substring(28,32));

        if (!Header.equals(this.header) || !zero.equals(this.zeros) || this.neighborID != receivedID) { 
            throw new RuntimeException(); 
        }

    }

    //send a message to the output stream
    public void sendMessage(String msg) {
        try {
            //stream write the message
            out.writeObject(msg);
            out.flush();
        }
        catch(IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
