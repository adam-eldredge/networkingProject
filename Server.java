import java.net.*;
import java.io.*;

public class Server {
    private static final int sPort = 8000; //The server will be listening on this port number
    
    int numClients = 0;
    peerProcess peer = null;
    Handler[] clients = new Handler[numClients];
    

    public Server(peerProcess peer) {
        this.peer = peer;
    }

    public void run() throws Exception {
        System.out.println("The server is running.");
        ServerSocket listener = new ServerSocket(sPort);
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
                    handshake(in);

                    while(true) {
                        message = (String)in.readObject();
                        peer.handleMessage(message);

                        System.out.println("Receive message: " + message + " from client " + no);
                        //Capitalize all letters in the message
                        MESSAGE = message.toUpperCase();
                        //send MESSAGE back to the client
                        sendMessage(MESSAGE);
                    }
                }
                catch(ClassNotFoundException classnot){
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


        public void handshake(ObjectInputStream in) {
            try {
                /* RECEIVE HANDSHAKE */
                String clientmessage = (String)in.readObject();
                System.out.println("Handshake: " + clientmessage + " from client " + no);

                /* VERIFY HANDSHAKE */
                boolean verified = peer.verifyHandshakeResponse(clientmessage, true, 0 /* Expected client number */);

                if (verified) {
                    peer.sendHandshakeMessage();
                    System.out.println("Sent handshake");
                }
                else {
                    throw new RuntimeException();
                }

            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.err.println("Class not found");
            } catch (RuntimeException runtimeException) {
                System.err.println("Handshake was not verified");
            }
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