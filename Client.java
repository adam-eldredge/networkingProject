import java.net.*;
import java.sql.Connection;
import java.io.*;
import java.util.Arrays;

public class Client {

    Socket requestSocket; //socket connect to the server
    ObjectOutputStream out; //stream write to the socket
    ObjectInputStream in; //stream read from the socket
    String message; //message send to the server
    String MESSAGE; //capitalized message read from the server
    String hostName;
    int portNum;
    int connectionID; 
    peerProcess peer; // Parent peer of this client

    public Client(peerProcess p, String hostName, int portNum, int connectionID) {
        peer = p;
        this.hostName = hostName;
        this.portNum = portNum;
        this.connectionID = connectionID;
    }

    void run() {
        try {
            //create a socket to connect to the server
            requestSocket = new Socket("localhost", portNum);

            //initialize inputStream and outputStream
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(requestSocket.getInputStream());

            handshake(in);
            System.out.println("Connected to " + hostName + " in port " + portNum);
            try {
                while(true) {
                    System.out.println("Waiting for server Response");
                    message = (String) in.readObject();
                    peer.receiveMessage(message, out, connectionID);
                }
            }
        catch(/*ClassNotFoundException */ Exception classnot){
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
        finally{
            //Close connections
            try{
                in.close();
                out.close();
                requestSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    void handshake(ObjectInputStream in){
        //Send initial handshake message when you connect
        sendHandshakeMessage(peer.ID);
        System.out.println("Sent handshake");

        try{
            //Wait for handshake response from server
            String handshakeResponse = (String)in.readObject();
            System.out.println("Received handshake Response: " + handshakeResponse);

            //   Verify Handshake Response
            boolean verified = verifyHandshakeResponse(handshakeResponse, connectionID);

            if (verified) {
                System.out.println("Handshake verified.");
            }
            else {
                throw new RuntimeException();
            }

        }catch(IOException ioException){
            ioException.printStackTrace();
        }
        catch (ClassNotFoundException e ) {
            System.err.println("Class not found");
        }
        catch (RuntimeException e) {
            System.err.println("Handshake from server failed to verify");
        }
    }

    public void sendHandshakeMessage(int pID) {
        String header = "P2PFILESHARINGPROJ";
        String zeros = "0000000000";
        String id = String.valueOf(pID);

        //Do we want to send as byte[] or as String??
        String msgString = header + zeros + id;
        sendMessage(msgString);
    }

    // Handshake response verification
    public boolean verifyHandshakeResponse(String msg, int expectedID) {
        String Header = msg.substring(0,18);
        String zero = msg.substring(18,28); 
        int receivedID = Integer.parseInt(msg.substring(28,32));

        if (!Header.equals("P2PFILESHARINGPROJ") || !zero.equals("0000000000") || expectedID != receivedID) { return false; }

        return true;
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
