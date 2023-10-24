import java.net.*;
import java.io.*;

public class Server {
    
    int numClients = 0;
    peerProcess peer = null;
    int portNum;
    Handler[] clients = new Handler[numClients];
    

    public Server(peerProcess peer, int portNum) {
        this.peer = peer;
        this.portNum = portNum;
    }

    public void run() throws Exception {
        System.out.println("The server is running on port: " + portNum);
        ServerSocket listener = new ServerSocket(portNum);
        try {
            while(true) {
                int newNumClients = numClients + 1;
                Handler[] newClients = new Handler[newNumClients];
                
                for (int i = 0; i < numClients; i++) {
                    newClients[i] = clients[i];
                }

                newClients[numClients] = new Handler(listener.accept(), numClients, peer);
                newClients[numClients].start();

                System.out.println("Client " + (numClients) + " is connected!");

                this.numClients = newNumClients;
                this.clients = newClients;
            }
        } finally {
            listener.close();
        }
    }

    /**
    * A handler thread class. Handlers are spawned from the listening
    * loop and are responsible for dealing with a single client's requests.
    */
    private static class Handler extends Thread {
        private String message; //message received from the client
        private String MESSAGE; //uppercase message send to the client
        private Socket connection;
        private ObjectInputStream in; //stream read from the socket
        private ObjectOutputStream out; //stream write to the socket
        private int no; //The index number of the client
        private int clientPeerID;
        private peerProcess peer; // Parent peer of the server

        public Handler(Socket connection, int no, peerProcess peer) {
            this.connection = connection;
            this.no = no;
            this.peer = peer;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                try{
                    message = (String) in.readObject();
                    handshake(message);
                    while(true) {
                        message = (String) in.readObject();
                        peer.receiveMessage(message, out);
                    }
                }
                catch(/*ClassNotFoundException */ Exception classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + no);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + no);
                }
            }
        }


        public void handshake(String clientmessage) {
            try {
                /* RECEIVE HANDSHAKE */
                System.out.println("Handshake: " + clientmessage + " from client " + no);

                /* VERIFY HANDSHAKE */
                boolean verified = verifyHandshakeResponse(clientmessage);

                if (verified) {
                    System.out.println("Handshake verified.");
                    sendHandshakeMessage(peer.ID);
                    System.out.println("Sent handshake");
                }
                else {
                    throw new RuntimeException();
                }
            } catch (RuntimeException runtimeException) {
                System.err.println("Handshake was not verified");
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
        public boolean verifyHandshakeResponse(String msg) {
            String Header = msg.substring(0,18);
            String zero = msg.substring(18,28); 
    
            if (!Header.equals("P2PFILESHARINGPROJ") || !zero.equals("0000000000")) { return false; }
    
            return true;
        }

        //send a message to the output stream
        public void sendMessage(String msg) {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client " + no);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        } 
    }
}